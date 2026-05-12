package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.AggregateSessionsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/sessions/aggregate?groupBy={day|week|month}&…`** —
 * server-side `COUNT(*) + SUM(driving_time_ms)` aggregation over the same filter set
 * `GET /api/1/sessions` supports. Each bucket carries both metrics so the dashboard's
 * "Sessions per …" and "Driving time per …" charts share a single request.
 *
 * `groupBy` is required; any missing or unknown value yields `400 Bad Request`.
 */
@OptIn(ExperimentalKtorApi::class)
class AggregateSessionsController(
  application: Application,
  aggregateSessionsUseCase: AggregateSessionsUseCase,
) {
  init {
    application.routing {
      get<SessionRoutes.Aggregate> {
          val groupByParam =
            call.request.queryParameters["groupBy"]
              ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing query parameter 'groupBy'"),
              )
          val groupBy =
            runCatching { parseSessionGroupBy(groupByParam) }
              .getOrElse {
                return@get call.respond(
                  HttpStatusCode.BadRequest,
                  mapOf("error" to (it.message ?: "Invalid groupBy")),
                )
              }
          val criteria = SessionSearchCriteria.fromParameters(call.request.queryParameters)
          call.respond(
            SessionAggregateResource.fromDomain(
              aggregateSessionsUseCase.aggregate(criteria, groupBy)
            )
          )
        }
        .describe {
          summary = "Aggregate sessions by time bucket"
          description =
            """
            Returns per-bucket `count` of sessions and `drivingTimeMs` sum, grouped by the
            supplied `groupBy` time dimension. Filters mirror `GET /api/1/sessions`
            (`car`, `track`, `simulator`, `from`, `to`).

            Bucket keys are formatted in UTC: `YYYY-MM-DD` for day/week (week starts Monday per
            the DB's `DATE_TRUNC`), `YYYY-MM` for month. Sessions with a null `started_at` are
            dropped — they have no timeline position.

            Empty buckets are omitted; the client fills any zero-count gaps it needs for layout.
            """
              .trimIndent()

          parameters {
            query("groupBy") {
              description = "Required. One of: `day`, `week`, `month`. Case-insensitive."
              required = true
            }
            query("from") {
              description = "Inclusive lower bound for `startedAt` as an ISO-8601 instant."
              required = false
            }
            query("to") {
              description = "Inclusive upper bound for `startedAt` as an ISO-8601 instant."
              required = false
            }
            query("car") {
              description = "Exact car name to constrain to, e.g. `Ferrari 296 GT3`."
              required = false
            }
            query("track") {
              description = "Exact track name to constrain to, e.g. `Snetterton Circuit`."
              required = false
            }
            query("simulator") {
              description = "Simulator to constrain to. Allowed values: `ACC`, `F1`."
              required = false
            }
          }

          responses {
            HttpStatusCode.OK { description = "Aggregate result with one bucket per time unit." }
            HttpStatusCode.BadRequest {
              description = "`groupBy` is missing or not one of the supported values."
            }
          }
        }
    }
  }
}
