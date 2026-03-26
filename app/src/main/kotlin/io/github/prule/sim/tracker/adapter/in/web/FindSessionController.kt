package io.github.prule.acc.client.app.io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`.FindSessionCommand
import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`.FindSessionUseCase
import io.github.prule.sim.tracker.adapter.`in`.web.SessionLinkFactory
import io.github.prule.sim.tracker.adapter.`in`.web.SessionResource
import io.github.prule.sim.tracker.adapter.`in`.web.SessionRoutes
import io.github.prule.sim.tracker.application.domain.model.Uid
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class FindSessionController(
    application: Application,
    findSessionUseCase: FindSessionUseCase,
) {
  init {
    application.routing {
      get<SessionRoutes.SessionId> { id ->
        call.respond(
            SessionResource.fromDomain(
                findSessionUseCase.findSession(FindSessionCommand(Uid(id.uid))),
                SessionLinkFactory(application),
            )
        )
      }
    }
  }
}
