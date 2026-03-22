package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.domain.service

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionId
import io.github.prule.sim.tracker.application.domain.model.Uid
import io.github.prule.sim.tracker.application.port.`in`.CreateSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.CreateSessionUseCase
import io.github.prule.sim.tracker.application.port.out.CreateSessionPort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CreateSessionService(
    private val createSessionPort: CreateSessionPort,
) : CreateSessionUseCase {
    override fun createSession(command: CreateSessionCommand): Session =
        transaction {
            val session =
                Session(
                    id = SessionId(0),
                    uid = Uid(),
                    startedAt = null,
                    endedAt = null,
                    simulator = command.simulator,
                    track = command.track,
                    car = command.car,
                    sessionType = command.sessionType,
                )
            createSessionPort.create(session)
        }
}
