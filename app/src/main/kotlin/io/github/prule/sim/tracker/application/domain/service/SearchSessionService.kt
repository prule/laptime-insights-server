package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.domain.service

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.`in`.SearchSessionUseCase
import io.github.prule.sim.tracker.application.port.out.SearchSessionPort
import io.github.prule.sim.tracker.utils.data.Page

class SearchSessionService(
    private val searchSessionPort: SearchSessionPort,
) : SearchSessionUseCase {
    override fun searchSessions(criteria: SessionSearchCriteria): Page<Session> {
        val page = searchSessionPort.search(criteria)
        return page
    }
}
