package io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Session

interface CreateSessionUseCase {
    fun createSession(command: CreateSessionCommand): Session
}
