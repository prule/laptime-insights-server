package com.github.prule.laptimeinsights.application.domain.model

import com.github.prule.laptimeinsights.tracker.utils.data.SearchCriteria

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
