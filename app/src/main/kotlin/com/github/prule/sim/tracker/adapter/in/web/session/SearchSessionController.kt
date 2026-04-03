package com.github.prule.sim.tracker.adapter.`in`.web.session

import com.github.prule.sim.tracker.adapter.`in`.web.toPageRequest
import com.github.prule.sim.tracker.adapter.`in`.web.toSort
import com.github.prule.sim.tracker.application.domain.model.Car
import com.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import com.github.prule.sim.tracker.application.domain.model.Simulator
import com.github.prule.sim.tracker.application.domain.model.Track
import com.github.prule.sim.tracker.application.port.`in`.session.SearchSessionUseCase
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class SearchSessionController(
    application: Application,
    searchSessionUseCase: SearchSessionUseCase,
) {
  init {
    application.routing {
      get<SessionRoutes> {
        call.respond(
            searchSessionUseCase
                .searchSessions(
                    SessionSearchCriteria.fromParameters(call.request.queryParameters),
                    call.request.toPageRequest(),
                    call.request.toSort(),
                )
                .map {
                  SessionResource.fromDomain(
                      it,
                      SessionLinkFactory(application),
                  )
                }
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
