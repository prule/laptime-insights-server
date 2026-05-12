package com.github.prule.laptimeinsights.application.domain.model

import kotlinx.serialization.Serializable

/**
 * Time-bucket dimensions for session aggregations. Sessions only support time grouping (there is
 * no useful per-track bucket for sessions — that lives on the laps aggregate).
 *
 * Keys are emitted as UTC strings (`YYYY-MM-DD` for day/week, `YYYY-MM` for month).
 */
@Serializable
enum class SessionAggregateGroupBy {
  DAY,
  WEEK,
  MONTH,
}

/**
 * Per-bucket aggregate over sessions. `count` is the number of sessions starting in this bucket;
 * `drivingTimeMs` is the sum of player-car driving time across those sessions. Both metrics are
 * emitted from a single SQL `GROUP BY` so the dashboard's "Sessions per …" and "Driving time per …"
 * charts share a single endpoint.
 */
@Serializable
data class SessionAggregateBucket(
  val key: String,
  val count: Long,
  val drivingTimeMs: Long,
)

/** Sparse result — empty buckets are omitted; the client fills any zero-count gaps for layout. */
@Serializable
data class SessionAggregate(
  val groupBy: SessionAggregateGroupBy,
  val buckets: List<SessionAggregateBucket>,
)
