package com.github.prule.sim.tracker.application.domain.service.session

import com.github.prule.sim.tracker.application.domain.model.Session
import com.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import com.github.prule.sim.tracker.application.port.`in`.session.SearchSessionUseCase
import com.github.prule.sim.tracker.application.port.out.session.SearchSessionPort
import com.github.prule.sim.tracker.utils.data.Page
import com.github.prule.sim.tracker.utils.data.PageRequest
import com.github.prule.sim.tracker.utils.data.Sort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SearchSessionService(
    private val searchSessionPort: SearchSessionPort,
) : SearchSessionUseCase {
  override fun searchSessions(
      criteria: SessionSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Session> = transaction { searchSessionPort.search(criteria, pageRequest, sort) }
}
