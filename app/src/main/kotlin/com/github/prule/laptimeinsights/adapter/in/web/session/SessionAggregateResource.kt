package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.SessionAggregate
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateBucket
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateGroupBy
import kotlinx.serialization.Serializable

/**
 * Wire shape for `GET /api/1/sessions/aggregate`. The dimension is exposed as a lower-case string
 * (`"day"`, `"week"`, `"month"`) — that's how the client supplies it and round-tripping the casing
 * keeps things simple.
 */
@Serializable
data class SessionAggregateResource(
  val groupBy: String,
  val buckets: List<SessionAggregateBucket>,
) {
  companion object {
    fun fromDomain(aggregate: SessionAggregate): SessionAggregateResource =
      SessionAggregateResource(
        groupBy = aggregate.groupBy.name.lowercase(),
        buckets = aggregate.buckets,
      )
  }
}

fun parseSessionGroupBy(value: String): SessionAggregateGroupBy =
  SessionAggregateGroupBy.entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    ?: error("Unsupported groupBy: '$value'. Expected one of: day, week, month.")
