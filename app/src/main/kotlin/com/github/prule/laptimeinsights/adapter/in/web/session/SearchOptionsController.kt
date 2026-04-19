package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.SearchSessionOptionsUseCase
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.routing

class SearchOptionsController(
    application: Application,
    searchSessionOptionsUseCase: SearchSessionOptionsUseCase,
) {
  init {
    application.routing {
      get<SessionRoutes.Options> {
        call.respond(
            SessionOptionsResource.fromDomain(
                searchSessionOptionsUseCase.options(
                    SessionSearchCriteria.fromParameters(call.request.queryParameters),
                ),
                SessionOptionsLinkFactory(application),
            )
        )
      }
    }
  }
}
