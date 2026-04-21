package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.adapter.`in`.web.toPageRequest
import com.github.prule.laptimeinsights.adapter.`in`.web.toSort
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.SearchLapUseCase
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class SearchLapController(application: Application, searchLapUseCase: SearchLapUseCase) {
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
            .map { LapResource.fromDomain(it, LapLinkFactory(application)) }
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
