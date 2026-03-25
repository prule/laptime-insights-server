package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.domain.service

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.`in`.SearchSessionUseCase
import io.github.prule.sim.tracker.application.port.out.SearchSessionPort
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort
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
