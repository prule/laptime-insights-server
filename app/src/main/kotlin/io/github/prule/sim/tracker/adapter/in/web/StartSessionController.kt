package io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.application.port.`in`.StartSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.StartSessionUseCase
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
                    StartSessionCommand(
                        uid = start.parent.uid,
                        startedAt = request.startedAt,
                    ),
                ),
            ),
        )
      }
    }
  }
}

@Serializable data class StartSessionRequest(val startedAt: Instant)
