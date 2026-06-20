package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionEnded
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.EndSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.EndSessionUseCase
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EndSessionService(
  private val updateSessionPort: UpdateSessionPort,
  private val searchSessionPort: SearchSessionPort,
  private val eventPort: EventPort,
) : EndSessionUseCase {
  override fun endSession(command: EndSessionCommand): Session {
    val endedSession = transaction {
      val session =
        searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
          ?: throw NotFoundException()
      // Idempotent: Session.end keeps the first recorded end time.
      session.end(command.endedAt)
      updateSessionPort.update(session)
    }
    eventPort.emit(SessionEnded(endedSession))
    return endedSession
  }
}
