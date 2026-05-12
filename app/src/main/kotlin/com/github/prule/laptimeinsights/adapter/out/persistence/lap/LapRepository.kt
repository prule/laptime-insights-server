package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateBucket
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateGroupBy
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
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.RowNumber
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.alias
import org.jetbrains.exposed.v1.core.count
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.longLiteral
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.datetime.KotlinInstantColumnType
import org.jetbrains.exposed.v1.jdbc.Query
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll

class LapRepository(private val mapper: LapMapper) :
  FindByIdRepository<LapEntity, Long>,
  FindByCriteriaRepository<LapEntity>,
  SearchRepository<LapEntity, LapSearchCriteria> {
  override fun findOneOrNull(id: Long): LapEntity? = LapEntity.findById(id)

  fun create(lap: Lap): LapEntity = LapEntity.new { mapper.toEntity(lap, this) }

  override fun searchForOne(criteria: LapSearchCriteria, sort: Sort): LapEntity? {
    return resolvedQuery(criteria).firstOrNull(sort, LapEntity.sortableFields) {
      LapEntity.wrapRow(it)
    }
  }

  override fun search(
    criteria: LapSearchCriteria,
    pageRequest: PageRequest,
    sort: Sort,
  ): Page<LapEntity> {
    return resolvedQuery(criteria).paginate(pageRequest, sort, LapEntity.sortableFields) {
      LapEntity.wrapRow(it)
    }
  }

  fun update(lap: Lap): LapEntity {
    return LapEntity.findByIdAndUpdate(lap.id.value) { mapper.toEntity(lap, it) }
      ?: throw NotFoundException("Lap not found")
  }

  private fun resolvedQuery(criteria: LapSearchCriteria): Query =
    if (criteria.allTimeBest?.value == true) bestPerTrackQuery(criteria) else criteria.toQuery()

  /**
   * SQL-side dedup for `allTimeBest = true`: ranks each filtered row with `ROW_NUMBER()`
   * partitioned by `LAP.track` and ordered by `lap_time, id`, then keeps `rn = 1`. The `id`
   * tiebreaker makes the pick deterministic when two laps share the same time on the same track.
   * Rows with a null track are dropped — a "best per track" with no track to attribute it to has no
   * meaning.
   *
   * Returns a regular `LapTable` query whose rows are the best laps; the caller still applies sort
   * and pagination at the DB level via `paginate(...)`.
   */
  private fun bestPerTrackQuery(criteria: LapSearchCriteria): Query {
    val rnAlias =
      RowNumber()
        .over()
        .partitionBy(LapTable.track)
        .orderBy(LapTable.lapTime to SortOrder.ASC, LapTable.id to SortOrder.ASC)
        .alias("rn")

    val ranked =
      criteria
        .toQuery()
        .andWhere { LapTable.track.isNotNull() }
        .adjustSelect { select(LapTable.id, rnAlias) }
        .alias("ranked")

    val bestIds = ranked.select(ranked[LapTable.id]).where { ranked[rnAlias] eq longLiteral(1L) }

    return LapTable.selectAll().where { LapTable.id inSubQuery bestIds }
  }

  /**
   * Server-side `COUNT(*) ... GROUP BY <dimension>` over the filtered laps. `TRACK` groups by the
   * lap's track column (rows with a null track are dropped). The time dimensions truncate
   * `recorded_at` via `DATE_TRUNC` and emit a stable string key (`YYYY-MM-DD` for day/week,
   * `YYYY-MM` for month) computed in UTC.
   *
   * Empty buckets are not represented — the caller fills any zero-count gaps it needs for layout.
   */
  fun aggregate(
    criteria: LapSearchCriteria,
    groupBy: LapAggregateGroupBy,
  ): List<LapAggregateBucket> {
    val countExpr = LapTable.id.count()
    val baseQuery = criteria.toQuery()
    return when (groupBy) {
      LapAggregateGroupBy.TRACK -> {
        val q =
          baseQuery
            .andWhere { LapTable.track.isNotNull() }
            .adjustSelect { select(LapTable.track, countExpr) }
            .groupBy(LapTable.track)
        q.map { row -> LapAggregateBucket(key = row[LapTable.track]!!, count = row[countExpr]) }
      }
      LapAggregateGroupBy.DAY,
      LapAggregateGroupBy.WEEK,
      LapAggregateGroupBy.MONTH -> {
        val truncExpr = dateTrunc(groupBy.sqlUnit, LapTable.recordedAt)
        val q =
          baseQuery
            .adjustSelect { select(truncExpr, countExpr) }
            .groupBy(truncExpr)
        q.map { row ->
          LapAggregateBucket(key = formatBucketKey(row[truncExpr], groupBy), count = row[countExpr])
        }
      }
    }
  }
}

private val LapAggregateGroupBy.sqlUnit: String
  get() =
    when (this) {
      LapAggregateGroupBy.DAY -> "DAY"
      LapAggregateGroupBy.WEEK -> "WEEK"
      LapAggregateGroupBy.MONTH -> "MONTH"
      LapAggregateGroupBy.TRACK -> error("TRACK does not map to a SQL time unit")
    }

private fun dateTrunc(unit: String, expr: Expression<Instant>): Expression<Instant> =
  CustomFunction("DATE_TRUNC", KotlinInstantColumnType(), stringLiteral(unit), expr)

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")

/**
 * Bucket-key formatting uses the system default TZ on purpose. Exposed's `InstantColumnType`
 * stores `Instant`s as TZ-less wall-clock values converted via `TimeZone.currentSystemDefault()`
 * and reads them back as `Instant`s that are then re-offset against the system TZ, so the only
 * way to recover the truncated wall-clock day is to format the round-tripped Instant against the
 * same zone. In a single-user setup the JVM and browser TZ coincide, so the client sees the day
 * the player perceived the lap on.
 */
private fun formatBucketKey(instant: Instant, groupBy: LapAggregateGroupBy): String {
  val localDate = instant.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  return if (groupBy == LapAggregateGroupBy.MONTH) localDate.format(MONTH_FORMAT)
  else localDate.toString()
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
