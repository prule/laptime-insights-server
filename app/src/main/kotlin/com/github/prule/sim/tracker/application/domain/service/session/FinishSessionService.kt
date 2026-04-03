package com.github.prule.sim.tracker.application.domain.service.session

import com.github.prule.sim.tracker.application.domain.model.Session
import com.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import com.github.prule.sim.tracker.application.port.`in`.session.FinishSessionCommand
import com.github.prule.sim.tracker.application.port.`in`.session.FinishSessionUseCase
import com.github.prule.sim.tracker.application.port.out.session.SearchSessionPort
import com.github.prule.sim.tracker.application.port.out.session.UpdateSessionPort
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FinishSessionService(
    private val updateSessionPort: UpdateSessionPort,
    private val searchSessionPort: SearchSessionPort,
) : FinishSessionUseCase {
  override fun finishSession(command: FinishSessionCommand): Session = transaction {
    val session =
        searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
            ?: throw NotFoundException()
    updateSessionPort.update(session.copy(finishedAt = command.finishedAt))
  }
}
