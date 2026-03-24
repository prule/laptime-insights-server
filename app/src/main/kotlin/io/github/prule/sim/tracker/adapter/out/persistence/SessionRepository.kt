package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.utils.data.FindByCriteriaRepository
import io.github.prule.sim.tracker.utils.data.FindByIdRepository
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.SearchRepository
import io.github.prule.sim.tracker.utils.data.Sort
import io.github.prule.sim.tracker.utils.data.exposed.firstOrNull
import io.github.prule.sim.tracker.utils.data.exposed.paginate
import org.jetbrains.exposed.v1.core.eq
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
}

fun SessionSearchCriteria.toQuery(): Query {
  val query = SessionTable.selectAll()

  car?.let { query.andWhere { SessionTable.car eq it.value } }

  return query
}
