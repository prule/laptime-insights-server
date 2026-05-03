package com.github.prule.laptimeinsights.application.domain.model

import com.github.prule.laptimeinsights.tracker.utils.data.SearchCriteria

/**
 * Filter spec for lap search.
 *
 * `car`, `track` and `simulator` describe attributes of the **owning session**
 * — they trigger a join to the SESSION table at the persistence layer. All
 * other fields filter LAP-table columns directly.
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
) : SearchCriteria {
  companion object
}
