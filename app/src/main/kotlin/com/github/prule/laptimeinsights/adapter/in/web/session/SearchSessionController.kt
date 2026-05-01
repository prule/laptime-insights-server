package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.toPageRequest
import com.github.prule.laptimeinsights.adapter.`in`.web.toSort
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.SearchSessionUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import kotlin.time.Instant

/**
 * REST controller exposing **`GET /api/1/sessions`** — the paginated, filterable search endpoint
 * for racing sessions.
 *
 * The route is wired into Ktor's [Resources] plugin via [SessionRoutes] and decorated with the Ktor
 * OpenAPI `describe { }` DSL so the operation, its query parameters and its response schema are
 * picked up by `/openapi` and rendered in `/swaggerUI`.
 *
 * Filter parameters are parsed from the query string by [SessionSearchCriteria.fromParameters];
 * paging and sorting are parsed by [toPageRequest] / [toSort] (see `RoutingRequestExtension.kt`).
 */
@OptIn(ExperimentalKtorApi::class)
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
              .map { SessionResource.fromDomain(it, SessionLinkFactory(application)) }
          )
        }
        .describe {
          summary = "Search sessions"
          description =
            """
            Returns a paginated list of racing sessions matching the supplied filter criteria.

            All filters are optional and combined with logical AND — omit a parameter to leave that
            dimension unconstrained. Results are wrapped in the standard paged response and each
            item carries HATEOAS `_links` (e.g. `self`) so clients can navigate to a single session
            without constructing URLs themselves.
            """
              .trimIndent()

          parameters {
            query("id") {
              description = "Internal numeric session id (rarely used by clients — prefer `uid`)."
              required = false
            }
            query("uid") {
              description = "Public session UID (UUID-style identifier exposed in API responses)."
              required = false
            }
            query("car") {
              description = "Exact car name to filter by, e.g. `Ferrari 296 GT3`."
              required = false
            }
            query("track") {
              description = "Exact track name to filter by, e.g. `Snetterton Circuit`."
              required = false
            }
            query("simulator") {
              description = "Simulator the session was recorded in. Allowed values: `ACC`, `F1`."
              required = false
            }
            query("from") {
              description =
                "Inclusive lower bound for the session start time, as an ISO-8601 instant " +
                  "(e.g. `2026-04-11T00:00:00Z`)."
              required = false
            }
            query("to") {
              description =
                "Exclusive upper bound for the session start time, as an ISO-8601 instant " +
                  "(e.g. `2026-04-13T00:00:00Z`)."
              required = false
            }
            query("page") {
              description = "1-based page number to return. Defaults to `1` when omitted."
              required = false
            }
            query("size") {
              description = "Maximum number of items per page. Defaults to `25` when omitted."
              required = false
            }
            query("sort") {
              description =
                "Sort specification as a comma-separated list of `field:ORDER` pairs, where " +
                  "`ORDER` is `ASC` or `DESC` (e.g. `startedAt:DESC,id:ASC`). Omit for no " +
                  "explicit sort."
              required = false
            }
          }

          responses {
            HttpStatusCode.OK {
              description =
                "A page of sessions matching the supplied criteria. Each item is a " +
                  "`SessionResource` with HATEOAS `_links`."
            }
            HttpStatusCode.BadRequest {
              description =
                "One or more query parameters could not be parsed (e.g. malformed `from`/`to` " +
                  "instant, unknown `simulator` value, non-numeric `id`)."
            }
          }
        }
    }
  }
}

fun SessionSearchCriteria.Companion.fromParameters(parameters: Parameters): SessionSearchCriteria {
  return SessionSearchCriteria(
    car = parameters["car"]?.let { Car(it) },
    track = parameters["track"]?.let { Track(it) },
    simulator = parameters["simulator"]?.let { Simulator.valueOf(it) },
    from = parameters["from"]?.let { Instant.parse(it) },
    to = parameters["to"]?.let { Instant.parse(it) },
    uid = parameters["uid"]?.let { Uid(it) },
    id = parameters["id"]?.let { SessionId(it.toLong()) },
  )
}
