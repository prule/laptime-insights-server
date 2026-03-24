package io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.utils.data.Page

interface SearchSessionUseCase {
    fun searchSessions(criteria: SessionSearchCriteria): Page<Session>
}
