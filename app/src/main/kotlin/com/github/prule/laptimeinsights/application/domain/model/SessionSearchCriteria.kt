package com.github.prule.laptimeinsights.application.domain.model

import com.github.prule.laptimeinsights.tracker.utils.data.SearchCriteria
import kotlin.time.Instant

/**
 * Filter spec for session search.
 *
 * `from` / `to` constrain `SESSION.startedAt` as a half-open interval `[from, to)`: `from` is
 * inclusive, `to` is exclusive. Both are optional and combined with logical AND.
 */
data class SessionSearchCriteria(
  val id: SessionId? = null,
  val uid: Uid? = null,
  val car: Car? = null,
  val track: Track? = null,
  val simulator: Simulator? = null,
  val from: Instant? = null,
  val to: Instant? = null,
) : SearchCriteria {
  companion object
}
