package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionUpdated
import com.github.prule.laptimeinsights.application.port.`in`.session.UpdateSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.UpdateSessionUseCase
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

class UpdateSessionService(
  private val updateSessionPort: UpdateSessionPort,
  private val searchSessionPort: SearchSessionPort,
  private val eventPort: EventPort,
) : UpdateSessionUseCase {
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun update(command: UpdateSessionCommand): Session = transaction {
    logger.debug("Update session: $command")
    val session =
      searchSessionPort.searchForOne(SessionSearchCriteria(uid = command.uid))
        ?: throw NotFoundException(command.uid.toString())
    val updatedSession = updateSessionPort.update(command.copyToSession(session))
    runBlocking { eventPort.emit(SessionUpdated(updatedSession)) }
    updatedSession
  }
}
