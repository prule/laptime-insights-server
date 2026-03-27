package io.github.prule.sim.tracker.adapter.`in`.web.lap

import io.github.prule.sim.tracker.adapter.`in`.web.toPageRequest
import io.github.prule.sim.tracker.adapter.`in`.web.toSort
import io.github.prule.sim.tracker.application.domain.model.LapSearchCriteria
import io.github.prule.sim.tracker.application.domain.model.Uid
import io.github.prule.sim.tracker.application.port.`in`.lap.SearchLapUseCase
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class SearchLapController(
    application: Application,
    searchLapUseCase: SearchLapUseCase,
) {
  init {
    application.routing {
      get<LapRoutes> {
        call.respond(
            searchLapUseCase
                .searchLaps(
                    LapSearchCriteria.fromParameters(call.request.queryParameters),
                    call.request.toPageRequest(),
                    call.request.toSort(),
                )
                .map {
                  LapResource.fromDomain(
                      it,
                      LapLinkFactory(application),
                  )
                }
        )
      }
    }
  }
}

fun LapSearchCriteria.Companion.fromParameters(parameters: Parameters): LapSearchCriteria {
  return LapSearchCriteria(
      uid = parameters["uid"]?.let { Uid(it) },
      sessionUid = parameters["sessionUid"]?.let { Uid(it) },
  )
}
