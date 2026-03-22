package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.domain.service

import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`.CreateSessionCommand
import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`.CreateSessionUseCase
import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionId
import io.github.prule.sim.tracker.application.port.`in`.CreateLapCommand
import io.github.prule.sim.tracker.application.port.`in`.CreateLapUseCase
import io.github.prule.sim.tracker.application.port.`in`.CreateSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.CreateSessionUseCase
import io.github.prule.sim.tracker.application.port.out.CreateSessionPort

class CreateSessionService(
    private val createSessionPort: CreateSessionPort,
) : CreateSessionUseCase {
    override fun createSession(command: CreateSessionCommand) {
        val session =
            Session(
                id = SessionId(0),
                startedAt = command.startedAt,
                endedAt = null,
            )
        createSessionPort.create(command)
    }
}
