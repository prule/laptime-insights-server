package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import com.github.prule.laptimeinsights.tracker.utils.data.FindByCriteriaRepository
import com.github.prule.laptimeinsights.tracker.utils.data.FindByIdRepository
import com.github.prule.laptimeinsights.tracker.utils.data.Order
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.SearchRepository
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import com.github.prule.laptimeinsights.tracker.utils.data.exposed.firstOrNull
import com.github.prule.laptimeinsights.tracker.utils.data.exposed.paginate
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll

class LapRepository(private val mapper: LapMapper) :
  FindByIdRepository<LapEntity, Long>,
  FindByCriteriaRepository<LapEntity>,
  SearchRepository<LapEntity, LapSearchCriteria> {
  override fun findOneOrNull(id: Long): LapEntity? = LapEntity.findById(id)

  fun create(lap: Lap): LapEntity = LapEntity.new { mapper.toEntity(lap, this) }

  override fun searchForOne(criteria: LapSearchCriteria, sort: Sort): LapEntity? {
    if (criteria.allTimeBest?.value == true) {
      return bestPerTrack(criteria).sortedRows(sort).firstOrNull()?.let { LapEntity.wrapRow(it) }
    }
    return criteria.toQuery().firstOrNull(sort, LapEntity.sortableFields) { LapEntity.wrapRow(it) }
  }

  override fun search(
    criteria: LapSearchCriteria,
    pageRequest: PageRequest,
    sort: Sort,
  ): Page<LapEntity> {
    if (criteria.allTimeBest?.value == true) {
      val rows = bestPerTrack(criteria).sortedRows(sort)
      val total = rows.size.toLong()
      val from = ((pageRequest.page - 1) * pageRequest.size).coerceAtLeast(0)
      val to = (from + pageRequest.size).coerceAtMost(rows.size)
      val items = if (from >= rows.size) emptyList() else rows.subList(from, to)
      return Page(pageRequest, total, items.map { LapEntity.wrapRow(it) })
    }
    return criteria.toQuery().paginate(pageRequest, sort, LapEntity.sortableFields) {
      LapEntity.wrapRow(it)
    }
  }

  fun update(lap: Lap): LapEntity {
    return LapEntity.findByIdAndUpdate(lap.id.value) { mapper.toEntity(lap, it) }
      ?: throw NotFoundException("Lap not found")
  }

  /**
   * Compute-on-read for `allTimeBest = true`: pulls the rows matching the row-level filters and
   * keeps the fastest lap per `LAP.track`. Rows with a null track are dropped — a "best per track"
   * with no track to attribute it to has no meaning. Done in memory; the dashboard surface
   * (best-per-track) is small (~dozens of tracks per player), and this avoids window-function
   * gymnastics in Exposed.
   */
  private fun bestPerTrack(criteria: LapSearchCriteria): List<ResultRow> {
    return criteria
      .toQuery()
      .toList()
      .filter { it[LapTable.track] != null }
      .groupBy { it[LapTable.track] }
      .mapNotNull { (_, group) -> group.minByOrNull { it[LapTable.lapTime] } }
  }

  private fun List<ResultRow>.sortedRows(sort: Sort): List<ResultRow> {
    if (sort.fields.isEmpty()) return this
    val comparators =
      sort.fields.mapNotNull { spec ->
        val col = LapEntity.sortableFields.mapping[spec.field] as? Column<*> ?: return@mapNotNull null
        @Suppress("UNCHECKED_CAST") val typedCol = col as Column<Comparable<Any>>
        val nullSafe: Comparator<ResultRow> =
          Comparator { a, b ->
            val av = a[typedCol] as Comparable<Any>?
            val bv = b[typedCol] as Comparable<Any>?
            when {
              av == null && bv == null -> 0
              av == null -> 1
              bv == null -> -1
              else -> av.compareTo(bv)
            }
          }
        if (spec.order == Order.ASC) nullSafe else nullSafe.reversed()
      }
    if (comparators.isEmpty()) return this
    val combined = comparators.reduce { acc, c -> acc.then(c) }
    return sortedWith(combined)
  }
}

fun LapSearchCriteria.toQuery(): Query {
  // Only join the SESSION table when a session-scoped facet is in play. Avoids
  // the join cost on the common path (e.g. listing laps for a known session).
  val needsSessionJoin = car != null || track != null || simulator != null
  val source =
    if (needsSessionJoin) {
      LapTable.join(
        SessionTable,
        JoinType.INNER,
        onColumn = LapTable.sessionUid,
        otherColumn = SessionTable.uid,
      )
    } else {
      LapTable
    }
  val query = source.selectAll()

  id?.let { query.andWhere { LapTable.id eq it.value } }
  uid?.let { query.andWhere { LapTable.uid eq it.value } }

  sessionId?.let { query.andWhere { LapTable.sessionId eq it.value } }
  sessionUid?.let { query.andWhere { LapTable.sessionUid eq it.value } }

  carId?.let { query.andWhere { LapTable.carId eq it.value } }
  personalBest?.let { query.andWhere { LapTable.personalBest eq it.value } }
  validLap?.let { query.andWhere { LapTable.valid eq it.value } }
  playerLap?.let { query.andWhere { LapTable.playerLap eq it.value } }

  car?.let { query.andWhere { SessionTable.car eq it.value } }
  track?.let { query.andWhere { SessionTable.track eq it.value } }
  simulator?.let { query.andWhere { SessionTable.simulator eq it.name } }

  // Filter on LAP.recordedAt — inclusive lower / inclusive upper bound, mirrors
  // SessionRepository semantics for from/to.
  from?.let { query.andWhere { LapTable.recordedAt greaterEq it } }
  to?.let { query.andWhere { LapTable.recordedAt lessEq it } }

  return query
}
