package com.github.prule.sim.tracker.adapter.`in`.web.lap

import com.github.prule.sim.tracker.application.port.`in`.lap.CreateLapCommand
import com.github.prule.sim.tracker.application.port.`in`.lap.CreateLapUseCase
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.post
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class CreateLapController(
    application: Application,
    createLapUseCase: CreateLapUseCase,
) {
  init {
    application.routing {
      post<LapRoutes> {
        call.respond(
            LapResource.fromDomain(
                createLapUseCase.createLap(
                    call.receive<CreateLapCommand>(),
                ),
                LapLinkFactory(application),
            ),
        )
      }
    }
  }
}
