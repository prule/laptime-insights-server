package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionFinished
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionUseCase
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FinishSessionService(
  private val updateSessionPort: UpdateSessionPort,
  private val searchSessionPort: SearchSessionPort,
  private val eventPort: EventPort,
) : FinishSessionUseCase {
  override fun finishSession(command: FinishSessionCommand): Session {
    val finishedSession = transaction {
      val session =
        searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
          ?: throw NotFoundException()
      session.finish(command.finishedAt)
      updateSessionPort.update(session)
    }
    eventPort.emit(SessionFinished(finishedSession))
    return finishedSession
  }
}
