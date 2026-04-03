package com.github.prule.laptimeinsights.application.domain.model

import com.github.prule.laptimeinsights.tracker.utils.data.SearchCriteria
import kotlin.time.Instant

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
