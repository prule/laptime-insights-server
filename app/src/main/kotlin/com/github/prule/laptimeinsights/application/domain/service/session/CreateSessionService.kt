package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionUseCase
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.CreateSessionPort
import kotlin.time.Clock
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CreateSessionService(
  private val createSessionPort: CreateSessionPort,
  private val eventPort: EventPort,
) : CreateSessionUseCase {
  override fun createSession(command: CreateSessionCommand): Session = transaction {
    val session =
      Session(
        id = SessionId(0),
        uid = Uid(),
        startedAt = Clock.System.now(),
        finishedAt = null,
        simulator = command.simulator,
        track = command.track,
        car = command.car,
        sessionType = command.sessionType,
      )
    val savedSession = createSessionPort.create(session)
    runBlocking { eventPort.emit(SessionCreated(savedSession)) }
    savedSession
  }
}
