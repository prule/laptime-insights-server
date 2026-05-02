package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/laps/{uid}`** — the single-lap lookup endpoint.
 *
 * The path-parameter `uid` is the public lap identifier (the same value returned in the `uid` field
 * of `LapResource` and in the `self` HATEOAS link). Internally the use case resolves the UID via
 * `SearchLapPort.searchForOne` and raises `NotFoundException` if no matching lap exists; that is
 * mapped to **404 Not Found** by the `StatusPages` plugin in `App.kt`.
 *
 * The route is wired into Ktor's [Resources] plugin via [LapRoutes.LapId] and decorated with the
 * Ktor OpenAPI `describe { }` DSL so the operation is picked up by `/openapi` and rendered in
 * `/swaggerUI`.
 */
@OptIn(ExperimentalKtorApi::class)
class FindLapController(application: Application, findLapUseCase: FindLapUseCase) {
  init {
    application.routing {
      get<LapRoutes.LapId> { id ->
          call.respond(
            LapResource.fromDomain(
              findLapUseCase.findLap(FindLapCommand(Uid(id.uid))),
              LapLinkFactory(application),
            )
          )
        }
        .describe {
          summary = "Find a lap by UID"
          description =
            """
            Returns the single lap identified by its public UID.

            The response is a `LapResource` with HATEOAS `_links` (e.g. `self`). If no lap matches
            the supplied UID the endpoint returns `404 Not Found`.
            """
              .trimIndent()

          parameters {
            path("uid") {
              description =
                "Public lap UID (the value exposed in the `uid` field of `LapResource` and in " +
                  "`self` HATEOAS links)."
              required = true
            }
          }

          responses {
            HttpStatusCode.OK { description = "The lap with the supplied UID." }
            HttpStatusCode.NotFound { description = "No lap exists with the supplied UID." }
          }
        }
    }
  }
}
