package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.adapter.`in`.web.toPageRequest
import com.github.prule.laptimeinsights.adapter.`in`.web.toSort
import com.github.prule.laptimeinsights.application.domain.model.AllTimeBest
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.PlayerLap
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
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
import kotlin.time.Instant

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
              .withSortable(Lap.SORTABLE_FIELDS)
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
            query("playerLap") {
              description =
                "If `true`, return only laps recorded by the player's car (the focused car of " +
                  "the owning session). If `false`, return only competitor laps. Omit to ignore. " +
                  "Values that don't parse as a strict boolean are silently ignored."
              required = false
            }
            query("allTimeBest") {
              description =
                "If `true`, post-filter the matched rows to keep only the fastest lap per " +
                  "`track` (rows with no track are dropped). Pair with `playerLap=true` and " +
                  "`validLap=true` for the player's all-time best per track. `false` or omitted " +
                  "leaves results unchanged."
              required = false
            }
            query("carId") {
              description =
                "Integer car number. Restricts results to laps recorded by the specified car " +
                  "within a session. Non-integer values are silently ignored."
              required = false
            }
            query("car") {
              description =
                "Exact car name of the owning session, e.g. `Ferrari 488 GT3`. Triggers a " +
                  "join to SESSION at the persistence layer."
              required = false
            }
            query("track") {
              description =
                "Exact track name of the owning session, e.g. `Snetterton`. Triggers a " +
                  "join to SESSION at the persistence layer."
              required = false
            }
            query("simulator") {
              description =
                "Simulator of the owning session. Allowed values: `ACC`, `F1`. Triggers a " +
                  "join to SESSION at the persistence layer."
              required = false
            }
            query("from") {
              description =
                "Inclusive lower bound for the lap's `recordedAt`, as an ISO-8601 instant " +
                  "(e.g. `2026-04-11T00:00:00Z`)."
              required = false
            }
            query("to") {
              description =
                "Inclusive upper bound for the lap's `recordedAt`, as an ISO-8601 instant " +
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
    carId = parameters["carId"]?.toIntOrNull()?.let { CarId(it) },
    personalBest = parameters["personalBest"]?.toBooleanStrictOrNull()?.let { PersonalBest(it) },
    validLap = parameters["validLap"]?.toBooleanStrictOrNull()?.let { ValidLap(it) },
    playerLap = parameters["playerLap"]?.toBooleanStrictOrNull()?.let { PlayerLap(it) },
    allTimeBest = parameters["allTimeBest"]?.toBooleanStrictOrNull()?.let { AllTimeBest(it) },
    car = parameters["car"]?.let { Car(it) },
    track = parameters["track"]?.let { Track(it) },
    simulator = parameters["simulator"]?.let { Simulator.valueOf(it) },
    from = parameters["from"]?.let { Instant.parse(it) },
    to = parameters["to"]?.let { Instant.parse(it) },
  )
}
