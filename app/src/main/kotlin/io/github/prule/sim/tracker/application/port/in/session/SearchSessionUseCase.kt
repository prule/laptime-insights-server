package io.github.prule.sim.tracker.application.port.`in`.session

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort

interface SearchSessionUseCase {
  fun searchSessions(
      criteria: SessionSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Session>
}
