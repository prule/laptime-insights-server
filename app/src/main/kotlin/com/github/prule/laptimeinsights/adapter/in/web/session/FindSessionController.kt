package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.FindSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FindSessionUseCase
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
