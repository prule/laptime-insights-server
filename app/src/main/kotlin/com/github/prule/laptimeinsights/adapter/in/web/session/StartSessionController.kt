package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlin.time.Instant
import kotlinx.serialization.Serializable

class StartSessionController(application: Application, startSessionUseCase: StartSessionUseCase) {
  init {
    application.routing {
      post<SessionRoutes.SessionId.Start> { start ->
        val request = call.receive<StartSessionRequest>()
        call.respond(
          SessionResource.fromDomain(
            startSessionUseCase.startSession(
              StartSessionCommand(uid = Uid(start.parent.uid), startedAt = request.startedAt)
            ),
            SessionLinkFactory(application),
          )
        )
      }
    }
  }
}

@Serializable data class StartSessionRequest(val startedAt: Instant)
