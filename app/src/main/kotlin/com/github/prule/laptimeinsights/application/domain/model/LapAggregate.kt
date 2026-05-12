package com.github.prule.laptimeinsights.application.domain.model

import kotlinx.serialization.Serializable

/**
 * Dimensions a lap aggregation can group by.
 *
 * `TRACK` produces one bucket per distinct `LAP.track` (rows with a null track are dropped). The
 * three time dimensions truncate `LAP.recordedAt` to the start of the unit and group by that:
 * - `DAY` → one bucket per calendar day.
 * - `WEEK` → one bucket per ISO week (Monday start).
 * - `MONTH` → one bucket per calendar month.
 *
 * Time-bucket keys are emitted as **UTC** strings (`YYYY-MM-DD` for day/week, `YYYY-MM` for month)
 * so they are unambiguous on the wire and trivially comparable on the client.
 */
@Serializable
enum class LapAggregateGroupBy {
  TRACK,
  DAY,
  WEEK,
  MONTH,
}

/**
 * Single bucket in a lap aggregation. `key` is the dimension value (track name or UTC-truncated
 * date string — see [LapAggregateGroupBy]); `count` is the number of laps matching the criteria
 * that fell into this bucket.
 */
@Serializable data class LapAggregateBucket(val key: String, val count: Long)

/**
 * Lap aggregation result. `buckets` is sparse — empty buckets are omitted, the client fills any
 * gaps it needs for layout.
 */
@Serializable
data class LapAggregate(val groupBy: LapAggregateGroupBy, val buckets: List<LapAggregateBucket>)
