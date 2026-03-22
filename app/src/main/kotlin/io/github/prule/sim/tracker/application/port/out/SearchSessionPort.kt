package io.github.prule.sim.tracker.application.port.out

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.utils.data.Page

interface SearchSessionPort {
    fun search(criteria: SessionSearchCriteria): Page<Session>
}
