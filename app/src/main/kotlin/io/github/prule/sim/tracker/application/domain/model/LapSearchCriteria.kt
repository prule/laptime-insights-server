package io.github.prule.sim.tracker.application.domain.model

import io.github.prule.sim.tracker.utils.data.SearchCriteria

data class LapSearchCriteria(
    val id: LapId? = null,
    val uid: Uid? = null,
    val sessionId: SessionId? = null,
    val sessionUid: Uid? = null,
    val personalBest: PersonalBest? = null,
    val validLap: ValidLap? = null,
) : SearchCriteria {
  companion object
}
