package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.lap.AggregateLapsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/laps/aggregate?groupBy={track|day|week|month}&…`** —
 * server-side `COUNT(*)` aggregation over the same filter set [LapSearchCriteria] supports for
 * `GET /api/1/laps`. The endpoint returns one bucket per dimension value (sparse — empty buckets
 * are omitted) so dashboards can render counts-per-track or counts-per-time-window without ever
 * pulling lap rows.
 *
 * `groupBy` is required; any missing or unknown value yields `400 Bad Request`.
 */
@OptIn(ExperimentalKtorApi::class)
class AggregateLapsController(
  application: Application,
  aggregateLapsUseCase: AggregateLapsUseCase,
) {
  init {
    application.routing {
      get<LapRoutes.Aggregate> {
          val groupByParam =
            call.request.queryParameters["groupBy"]
              ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing query parameter 'groupBy'"),
              )
          val groupBy =
            runCatching { parseGroupBy(groupByParam) }
              .getOrElse {
                return@get call.respond(
                  HttpStatusCode.BadRequest,
                  mapOf("error" to (it.message ?: "Invalid groupBy")),
                )
              }
          val criteria = LapSearchCriteria.fromParameters(call.request.queryParameters)
          call.respond(
            LapAggregateResource.fromDomain(aggregateLapsUseCase.aggregate(criteria, groupBy))
          )
        }
        .describe {
          summary = "Aggregate laps by dimension"
          description =
            """
            Returns a count of laps grouped by the supplied `groupBy` dimension. Filters mirror
            `GET /api/1/laps` exactly (`playerLap`, `validLap`, `from`, `to`, `track`, `car`,
            `simulator`, `sessionUid`, `carId`, `personalBest`).

            `groupBy=track` returns one bucket per distinct `LAP.track` (rows with a null track
            are dropped). The time dimensions truncate `recorded_at`:
            - `day` → key is `YYYY-MM-DD` (UTC).
            - `week` → key is `YYYY-MM-DD` of the week's start (DB-defined; H2 starts on Monday).
            - `month` → key is `YYYY-MM` (UTC).

            Empty buckets are omitted; the client fills any zero-count gaps it needs for layout.
            """
              .trimIndent()

          parameters {
            query("groupBy") {
              description =
                "Required. One of: `track`, `day`, `week`, `month`. Case-insensitive."
              required = true
            }
            query("playerLap") {
              description =
                "If `true`, restrict the aggregation to laps recorded by the player's car. " +
                  "Most dashboards pair this with `validLap=true`."
              required = false
            }
            query("validLap") {
              description = "If `true`, restrict to valid laps; if `false`, only invalid."
              required = false
            }
            query("from") {
              description = "Inclusive lower bound for `recordedAt` as an ISO-8601 instant."
              required = false
            }
            query("to") {
              description = "Inclusive upper bound for `recordedAt` as an ISO-8601 instant."
              required = false
            }
          }

          responses {
            HttpStatusCode.OK { description = "Aggregate result with one bucket per dimension value." }
            HttpStatusCode.BadRequest {
              description = "`groupBy` is missing or not one of the supported values."
            }
          }
        }
    }
  }
}
