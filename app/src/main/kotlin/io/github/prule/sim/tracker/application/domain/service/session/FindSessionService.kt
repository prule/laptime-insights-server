package io.github.prule.sim.tracker.application.domain.service.session

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.`in`.session.FindSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.session.FindSessionUseCase
import io.github.prule.sim.tracker.application.port.out.session.SearchSessionPort
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FindSessionService(
    private val searchSessionPort: SearchSessionPort,
) : FindSessionUseCase {
  override fun findSession(command: FindSessionCommand): Session = transaction {
    searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
        ?: throw NotFoundException()
  }
}
