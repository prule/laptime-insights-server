package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.FindSessionCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.FindSessionUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/sessions/{uid}`** — the single-session lookup endpoint.
 *
 * The path-parameter `uid` is the public session identifier (the same value returned in the `uid`
 * field of `SessionResource` and in the `self` HATEOAS link). Internally the use case resolves the
 * UID via `SearchSessionPort.searchForOne` and raises `NotFoundException` if no matching session
 * exists; that is mapped to **404 Not Found** by the `StatusPages` plugin in `App.kt`.
 *
 * The route is wired into Ktor's [Resources] plugin via [SessionRoutes.SessionId] and decorated
 * with the Ktor OpenAPI `describe { }` DSL so the operation is picked up by `/openapi` and rendered
 * in `/swaggerUI`.
 */
@OptIn(ExperimentalKtorApi::class)
class FindSessionController(application: Application, findSessionUseCase: FindSessionUseCase) {
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
        .describe {
          summary = "Find a session by UID"
          description =
            """
            Returns the single session identified by its public UID.

            The response is a `SessionResource` with HATEOAS `_links` (e.g. `self`). If no
            session matches the supplied UID the endpoint returns `404 Not Found`.
            """
              .trimIndent()

          parameters {
            path("uid") {
              description =
                "Public session UID (the value exposed in the `uid` field of `SessionResource` " +
                  "and in `self` HATEOAS links)."
              required = true
            }
          }

          responses {
            HttpStatusCode.OK { description = "The session with the supplied UID." }
            HttpStatusCode.NotFound { description = "No session exists with the supplied UID." }
          }
        }
    }
  }
}
