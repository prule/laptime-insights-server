package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
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
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class LapRepository(
    private val mapper: LapMapper,
) :
    FindByIdRepository<LapEntity, Long>,
    FindByCriteriaRepository<LapEntity>,
    SearchRepository<LapEntity, LapSearchCriteria> {
  override fun findOneOrNull(id: Long): LapEntity? = LapEntity.findById(id)

  fun create(lap: Lap): LapEntity = LapEntity.new { mapper.toEntity(lap, this) }

  override fun searchForOne(criteria: LapSearchCriteria, sort: Sort): LapEntity? {
    return criteria.toQuery().firstOrNull(sort, LapEntity.sortableFields) { LapEntity.wrapRow(it) }
  }

  override fun search(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<LapEntity> {
    return criteria.toQuery().paginate(pageRequest, sort, LapEntity.sortableFields) {
      LapEntity.wrapRow(it)
    }
  }

  fun update(lap: Lap): LapEntity {
    return LapEntity.findByIdAndUpdate(lap.id.value) { mapper.toEntity(lap, it) }
        ?: throw NotFoundException("Lap not found")
  }
}

fun LapSearchCriteria.toQuery(): Query {
  val query = LapTable.selectAll()

  id?.let { query.andWhere { LapTable.id eq it.value } }
  uid?.let { query.andWhere { LapTable.uid eq it.value } }

  sessionId?.let { query.andWhere { LapTable.sessionId eq it.value } }

  personalBest?.let { query.andWhere { LapTable.personalBest eq it.value } }
  validLap?.let { query.andWhere { LapTable.valid eq it.value } }

  return query
}
