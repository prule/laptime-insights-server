package io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.application.port.`in`.CreateSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.CreateSessionUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

class CreateSessionController(
    application: Application,
    createSessionUseCase: CreateSessionUseCase,
) {
  init {
    application.routing {
      post("/api/1/session") {
        call.respond(
            SessionResource.fromDomain(
                createSessionUseCase.createSession(
                    call.receive<CreateSessionCommand>(),
                ),
                SessionLinkFactory(application),
            ),
        )
      }
    }
  }
}
