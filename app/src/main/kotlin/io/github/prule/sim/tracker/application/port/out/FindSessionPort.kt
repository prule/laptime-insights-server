package io.github.prule.sim.tracker.application.port.out

import io.github.prule.sim.tracker.application.domain.model.SessionId

interface FindSessionPort {
    fun findById(id: SessionId)
}
