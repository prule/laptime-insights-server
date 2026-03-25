package io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.application.domain.model.Car
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.domain.model.Simulator
import io.github.prule.sim.tracker.application.domain.model.Track
import io.github.prule.sim.tracker.application.port.`in`.SearchSessionUseCase
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class SearchSessionController(
    application: Application,
    searchSessionUseCase: SearchSessionUseCase,
) {
  init {
    application.routing {
      get("/api/1/session") {
        call.respond(
            searchSessionUseCase
                .searchSessions(
                    SessionSearchCriteria.fromParameters(call.request.queryParameters),
                    call.request.toPageRequest(),
                    call.request.toSort(),
                )
                .map { SessionResource.fromDomain(it) }
        )
      }
    }
  }
}

fun SessionSearchCriteria.Companion.fromParameters(parameters: Parameters): SessionSearchCriteria {
  return SessionSearchCriteria(
      car = parameters["car"]?.let { Car(it) },
      track = parameters["track"]?.let { Track(it) },
      simulator = parameters["simulator"]?.let { Simulator.valueOf(it) },
  )
}
