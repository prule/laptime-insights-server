package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.domain.service

import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`.FindSessionCommand
import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`.FindSessionUseCase
import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.out.SearchSessionPort
import io.ktor.server.plugins.*
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FindSessionService(
    private val searchSessionPort: SearchSessionPort,
) : FindSessionUseCase {
  override fun findSession(command: FindSessionCommand): Session = transaction {
    searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
        ?: throw NotFoundException()
  }
}
