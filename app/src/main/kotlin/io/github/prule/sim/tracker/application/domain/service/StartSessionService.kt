package io.github.prule.sim.tracker.application.domain.service

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.`in`.StartSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.StartSessionUseCase
import io.github.prule.sim.tracker.application.port.out.SearchSessionPort
import io.github.prule.sim.tracker.application.port.out.UpdateSessionPort
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StartSessionService(
    private val updateSessionPort: UpdateSessionPort,
    private val searchSessionPort: SearchSessionPort,
) : StartSessionUseCase {
  override fun startSession(command: StartSessionCommand): Session = transaction {
    val session =
        searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
            ?: throw NotFoundException()
    updateSessionPort.update(session.copy(startedAt = command.startedAt))
  }
}
