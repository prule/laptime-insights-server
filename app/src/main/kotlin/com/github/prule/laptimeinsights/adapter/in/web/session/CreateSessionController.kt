package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionUseCase
import io.ktor.http.HttpStatusCode
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
          HttpStatusCode.Created,
          SessionResource.fromDomain(
            createSessionUseCase.createSession(call.receive<CreateSessionCommand>()),
            SessionLinkFactory(application),
          ),
        )
      }
    }
  }
}
