package io.github.prule.sim.tracker.adapter.`in`.web.session

import io.github.prule.sim.tracker.application.port.`in`.session.CreateSessionCommand
import io.github.prule.sim.tracker.application.port.`in`.session.CreateSessionUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class CreateSessionController(
    application: Application,
    createSessionUseCase: CreateSessionUseCase,
) {
  init {
    application.routing {
      post<SessionRoutes> {
        call.respond(
            SessionResource.Companion.fromDomain(
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
