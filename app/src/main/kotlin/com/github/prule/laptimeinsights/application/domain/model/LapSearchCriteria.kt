package com.github.prule.laptimeinsights.application.domain.model

import com.github.prule.laptimeinsights.tracker.utils.data.SearchCriteria
import kotlin.time.Instant

/**
 * Filter spec for lap search.
 *
 * `car`, `track` and `simulator` describe attributes of the **owning session**
 * — they trigger a join to the SESSION table at the persistence layer. All
 * other fields filter LAP-table columns directly.
 *
 * `from` / `to` constrain `LAP.recordedAt`: `from` is inclusive, `to` is
 * exclusive. Both are optional and combined with logical AND.
 */
data class LapSearchCriteria(
  val id: LapId? = null,
  val uid: Uid? = null,
  val sessionId: SessionId? = null,
  val sessionUid: Uid? = null,
  val personalBest: PersonalBest? = null,
  val validLap: ValidLap? = null,
  val car: Car? = null,
  val track: Track? = null,
  val simulator: Simulator? = null,
  val from: Instant? = null,
  val to: Instant? = null,
) : SearchCriteria {
  companion object
}
