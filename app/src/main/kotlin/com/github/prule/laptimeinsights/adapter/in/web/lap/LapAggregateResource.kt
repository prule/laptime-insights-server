package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.application.domain.model.LapAggregate
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateBucket
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateGroupBy
import kotlinx.serialization.Serializable

/**
 * Wire shape for `GET /api/1/laps/aggregate`. Mirrors [LapAggregate] but exposes the groupBy
 * dimension as a lower-case string (`"track"`, `"day"`, `"week"`, `"month"`) — that's how the
 * client passes it in, and re-using the casing keeps round-tripping painless.
 */
@Serializable
data class LapAggregateResource(val groupBy: String, val buckets: List<LapAggregateBucket>) {
  companion object {
    fun fromDomain(aggregate: LapAggregate): LapAggregateResource =
      LapAggregateResource(
        groupBy = aggregate.groupBy.name.lowercase(),
        buckets = aggregate.buckets,
      )
  }
}

/** Throws if [value] isn't one of the supported groupBy values. */
fun parseGroupBy(value: String): LapAggregateGroupBy =
  LapAggregateGroupBy.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    ?: error("Unsupported groupBy: '$value'. Expected one of: track, day, week, month.")
