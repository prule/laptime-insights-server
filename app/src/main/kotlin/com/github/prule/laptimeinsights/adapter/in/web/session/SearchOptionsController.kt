package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.SearchSessionOptionsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/sessions/options`** — the discovery endpoint that returns
 * the set of distinct values currently available for the session search filters (cars, tracks,
 * simulators, and the min/max recorded session timestamps).
 *
 * This is typically used by the frontend to populate filter dropdowns and constrain the date
 * picker. The endpoint accepts the same query parameters as `/api/1/sessions` so the returned
 * option set can be narrowed to be consistent with the user's current filter selections.
 *
 * The route is wired into Ktor's [Resources] plugin via [SessionRoutes.Options] and decorated with
 * the Ktor OpenAPI `describe { }` DSL so the operation is picked up by `/openapi` and rendered in
 * `/swaggerUI`.
 */
@OptIn(ExperimentalKtorApi::class)
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
                SessionSearchCriteria.fromParameters(call.request.queryParameters)
              ),
              SessionOptionsLinkFactory(application),
            )
          )
        }
        .describe {
          summary = "List session search options"
          description =
            """
            Returns the set of distinct filter values (cars, tracks, simulators) and the min/max
            session timestamps currently available in the database.

            All query parameters are optional and accept the same values as `GET /api/1/sessions`.
            When supplied, the returned option set is narrowed to be consistent with the supplied
            criteria — for example, passing `track=Snetterton Circuit` will return only the cars
            and simulators that have actually been recorded at that track. Omit parameters to
            receive the unfiltered set of options.

            The response carries HATEOAS `_links` (e.g. `self`).
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
              description = "Exact car name to constrain options to, e.g. `Ferrari 296 GT3`."
              required = false
            }
            query("track") {
              description = "Exact track name to constrain options to, e.g. `Snetterton Circuit`."
              required = false
            }
            query("simulator") {
              description = "Simulator to constrain options to. Allowed values: `ACC`, `F1`."
              required = false
            }
            query("from") {
              description =
                "Inclusive lower bound for session start time, as an ISO-8601 instant " +
                  "(e.g. `2026-04-11T00:00:00Z`)."
              required = false
            }
            query("to") {
              description =
                "Exclusive upper bound for session start time, as an ISO-8601 instant " +
                  "(e.g. `2026-04-13T00:00:00Z`)."
              required = false
            }
          }

          responses {
            HttpStatusCode.OK {
              description =
                "A `SessionOptionsResource` listing the distinct cars, tracks and simulators " +
                  "available, the min/max session timestamps, and HATEOAS `_links`."
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
