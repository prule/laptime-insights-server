package com.github.prule.sim.tracker.application.port.`in`.session

import com.github.prule.sim.tracker.application.domain.model.Session
import com.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import com.github.prule.sim.tracker.utils.data.Page
import com.github.prule.sim.tracker.utils.data.PageRequest
import com.github.prule.sim.tracker.utils.data.Sort

interface SearchSessionUseCase {
  fun searchSessions(
      criteria: SessionSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Session>
}
