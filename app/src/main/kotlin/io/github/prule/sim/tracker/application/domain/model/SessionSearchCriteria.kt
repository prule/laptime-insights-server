package io.github.prule.sim.tracker.application.domain.model

import io.github.prule.sim.tracker.utils.data.SearchCriteria

data class SessionSearchCriteria(
    val car: Car? = null,
    val track: Track? = null,
    val simulator: Simulator? = null,
) : SearchCriteria {
  companion object
}
