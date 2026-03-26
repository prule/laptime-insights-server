package io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.application.domain.model.Uid
import io.github.prule.sim.tracker.application.port.`in`.StartSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.StartSessionUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlin.time.Instant

class StartSessionController(application: Application, startSessionUseCase: StartSessionUseCase) {
  init {
    application.routing {
      post<SessionRoutes.SessionId.Start> { start ->
        val request = call.receive<StartSessionRequest>()
        call.respond(
            SessionResource.fromDomain(
                startSessionUseCase.startSession(
                    StartSessionCommand(
                        uid = Uid(start.parent.uid),
                        startedAt = request.startedAt,
                    ),
                ),
                SessionLinkFactory(application),
            ),
        )
      }
    }
  }
}

@Serializable data class StartSessionRequest(val startedAt: Instant)
