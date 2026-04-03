package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import com.github.prule.laptimeinsights.tracker.utils.data.FindByCriteriaRepository
import com.github.prule.laptimeinsights.tracker.utils.data.FindByIdRepository
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.SearchRepository
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import com.github.prule.laptimeinsights.tracker.utils.data.exposed.firstOrNull
import com.github.prule.laptimeinsights.tracker.utils.data.exposed.paginate
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class SessionRepository(
    private val mapper: SessionMapper,
) :
    FindByIdRepository<SessionEntity, Long>,
    FindByCriteriaRepository<SessionEntity>,
    SearchRepository<SessionEntity, SessionSearchCriteria> {
  override fun findOneOrNull(id: Long): SessionEntity? = SessionEntity.findById(id)

  fun create(session: Session): SessionEntity = SessionEntity.new { mapper.toEntity(session, this) }

  override fun searchForOne(criteria: SessionSearchCriteria, sort: Sort): SessionEntity? {
    return criteria.toQuery().firstOrNull(sort, SessionEntity.sortableFields) {
      SessionEntity.wrapRow(it)
    }
  }

  override fun search(
      criteria: SessionSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<SessionEntity> {
    return criteria.toQuery().paginate(pageRequest, sort, SessionEntity.sortableFields) {
      SessionEntity.wrapRow(it)
    }
  }

  fun update(session: Session): SessionEntity {
    return SessionEntity.findByIdAndUpdate(session.id.value) { mapper.toEntity(session, it) }
        ?: throw NotFoundException("Session not found")
  }
}

fun SessionSearchCriteria.toQuery(): Query {
  val query = SessionTable.selectAll()

  id?.let { query.andWhere { SessionTable.id eq it.value } }
  uid?.let { query.andWhere { SessionTable.uid eq it.value } }

  car?.let { query.andWhere { SessionTable.car eq it.value } }
  track?.let { query.andWhere { SessionTable.track eq it.value } }
  simulator?.let { query.andWhere { SessionTable.simulator eq it.name } }

  from?.let { query.andWhere { SessionTable.startedAt greaterEq it } }
  to?.let { query.andWhere { SessionTable.startedAt lessEq it } }

  return query
}
