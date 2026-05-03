package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapTelemetryUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1/laps/{uid}/telemetry`** — the full
 * spline-position-indexed telemetry trace for a single lap.
 */
@OptIn(ExperimentalKtorApi::class)
class FindLapTelemetryController(
  application: Application,
  findLapTelemetryUseCase: FindLapTelemetryUseCase,
) {
  init {
    application.routing {
      get<LapRoutes.LapId.Telemetry> { route ->
          val lapUid = Uid(route.parent.uid)
          call.respond(
            LapTelemetryResource.fromDomain(
              lapUid = lapUid,
              samples = findLapTelemetryUseCase.findByLapUid(lapUid),
              linkFactory = LapTelemetryLinkFactory(application),
            )
          )
        }
        .describe {
          summary = "Get full telemetry for a lap"
          description =
            """
            Returns every telemetry sample recorded during the lap, ordered by
            `splinePosition` ascending (0.0 → 1.0). Each sample carries
            `speedKph`, `gear`, `throttle` and `brake` per the lap-comparison
            specification.

            Samples are raw — no resampling or smoothing is applied. Callers
            that need a fixed bucket count (for example, to overlay two laps
            of different sample density) should resample on the client.
            """
              .trimIndent()

          parameters {
            path("uid") {
              description = "Public lap UID."
              required = true
            }
          }

          responses {
            HttpStatusCode.OK { description = "Telemetry trace for the lap." }
            HttpStatusCode.NotFound { description = "No lap exists with the supplied UID." }
          }
        }
    }
  }
}
