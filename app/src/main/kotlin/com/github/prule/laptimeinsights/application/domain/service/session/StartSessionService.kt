package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionUseCase
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class StartSessionService(
  private val updateSessionPort: UpdateSessionPort,
  private val searchSessionPort: SearchSessionPort,
) : StartSessionUseCase {
  override fun startSession(command: StartSessionCommand): Session = transaction {
    val session =
      searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
        ?: throw NotFoundException()
    session.start(command.startedAt)
    updateSessionPort.update(session)
  }
}
