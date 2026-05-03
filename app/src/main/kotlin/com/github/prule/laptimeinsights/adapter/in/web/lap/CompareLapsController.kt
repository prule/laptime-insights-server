package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.CompareLapsUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…`** — fetch the metadata +
 * raw telemetry for two laps so a client can render an overlay. See `docs/specs/lap-comparison.md`.
 */
@OptIn(ExperimentalKtorApi::class)
class CompareLapsController(application: Application, compareLapsUseCase: CompareLapsUseCase) {
  init {
    application.routing {
      get<LapRoutes.Compare> {
          val lap1Uid =
            call.request.queryParameters["lap1Uid"]
              ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing query parameter 'lap1Uid'"),
              )
          val lap2Uid =
            call.request.queryParameters["lap2Uid"]
              ?: return@get call.respond(
                HttpStatusCode.BadRequest,
                mapOf("error" to "Missing query parameter 'lap2Uid'"),
              )
          call.respond(
            LapComparisonResource.fromDomain(
              compareLapsUseCase.compare(Uid(lap1Uid), Uid(lap2Uid)),
              LapComparisonLinkFactory(application),
            )
          )
        }
        .describe {
          summary = "Compare two laps' telemetry"
          description =
            """
            Returns a `LapComparisonResource` carrying the metadata and raw
            telemetry samples for two laps. Each side keeps its own samples
            ordered by `splinePosition` so the client can align them visually
            without a join.

            Both laps must exist; either UID resolving to nothing yields
            `404 Not Found`. Laps from different tracks compare poorly but
            are not rejected — that judgement is left to the UI.
            """
              .trimIndent()

          parameters {
            query("lap1Uid") {
              description = "Public UID of the first lap (typically the driver's lap)."
              required = true
            }
            query("lap2Uid") {
              description =
                "Public UID of the second lap (typically a personal best or reference lap)."
              required = true
            }
          }

          responses {
            HttpStatusCode.OK { description = "Comparison payload with both laps and samples." }
            HttpStatusCode.BadRequest { description = "`lap1Uid` or `lap2Uid` was not provided." }
            HttpStatusCode.NotFound {
              description = "One or both UIDs do not match an existing lap."
            }
          }
        }
    }
  }
}
