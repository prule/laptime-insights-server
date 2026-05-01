package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.adapter.`in`.web.toPageRequest
import com.github.prule.laptimeinsights.adapter.`in`.web.toSort
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.lap.SearchLapUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/laps`** — the paginated, filterable search endpoint for
 * recorded laps.
 *
 * The most common use is to fetch every lap belonging to a single session, for example `GET
 * /api/1/laps?sessionUid=...&sort=lapNumber:ASC`, but `uid` is also supported for direct single-lap
 * lookup via the search interface.
 *
 * The route is wired into Ktor's [Resources] plugin via [LapRoutes] and decorated with the Ktor
 * OpenAPI `describe { }` DSL so the operation, its query parameters and its response are picked up
 * by `/openapi` and rendered in `/swaggerUI`.
 *
 * Filter parameters are parsed from the query string by [LapSearchCriteria.fromParameters]; paging
 * and sorting are parsed by [toPageRequest] / [toSort] (see `RoutingRequestExtension.kt`).
 */
@OptIn(ExperimentalKtorApi::class)
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
        .describe {
          summary = "Search laps"
          description =
            """
            Returns a paginated list of recorded laps matching the supplied filter criteria.

            All filters are optional and combined with logical AND — omit a parameter to leave
            that dimension unconstrained. The typical use is to list every lap for a session by
            passing `sessionUid` (often combined with `sort=lapNumber:ASC`).

            Results are wrapped in the standard paged response and each item carries HATEOAS
            `_links` (e.g. `self`).
            """
              .trimIndent()

          parameters {
            query("uid") {
              description =
                "Public lap UID. Returns at most one lap when supplied (use the `/api/1/laps/" +
                  "{uid}` endpoint for direct lookup unless you specifically need the paged " +
                  "response shape)."
              required = false
            }
            query("sessionUid") {
              description =
                "Public session UID — restricts results to laps recorded during this session. " +
                  "This is the most common filter."
              required = false
            }
            query("personalBest") {
              description =
                "If `true`, return only laps flagged as a personal best. If `false`, return " +
                  "only non-PB laps. Omit to ignore the flag entirely. Values that don't parse " +
                  "as a strict boolean are silently ignored."
              required = false
            }
            query("validLap") {
              description =
                "If `true`, return only valid laps. If `false`, return only invalid laps " +
                  "(e.g. cut-track penalties). Omit to ignore validity. Values that don't parse " +
                  "as a strict boolean are silently ignored."
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
                  "`ORDER` is `ASC` or `DESC` (e.g. `lapNumber:ASC`, `lapTime:ASC`). Omit for " +
                  "no explicit sort."
              required = false
            }
          }

          responses {
            HttpStatusCode.OK {
              description =
                "A page of laps matching the supplied criteria. Each item is a `LapResource` " +
                  "with HATEOAS `_links`."
            }
          }
        }
    }
  }
}

fun LapSearchCriteria.Companion.fromParameters(parameters: Parameters): LapSearchCriteria {
  return LapSearchCriteria(
    uid = parameters["uid"]?.let { Uid(it) },
    sessionUid = parameters["sessionUid"]?.let { Uid(it) },
    personalBest = parameters["personalBest"]?.toBooleanStrictOrNull()?.let { PersonalBest(it) },
    validLap = parameters["validLap"]?.toBooleanStrictOrNull()?.let { ValidLap(it) },
  )
}
