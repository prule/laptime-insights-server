package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /api/1/laps/{uid}/telemetry`.
 *
 * The samples are returned in spline-position order. `_links.self` is the canonical URL of this
 * telemetry trace; `_links.lap` points back at the owning lap so HATEOAS clients can navigate
 * without URL construction.
 */
@Serializable
data class LapTelemetryResource(
  val lapUid: Uid,
  val samples: List<TelemetrySample>,
  val _links: Map<String, String>,
) {
  companion object {
    fun fromDomain(
      lapUid: Uid,
      samples: List<TelemetrySample>,
      linkFactory: LapTelemetryLinkFactory,
    ): LapTelemetryResource =
      LapTelemetryResource(lapUid = lapUid, samples = samples, _links = linkFactory.build(lapUid))
  }
}

class LapTelemetryLinkFactory(private val application: Application) : LinkFactory<Uid> {
  override fun build(resource: Uid): Map<String, String> {
    val lap = LapRoutes.LapId(uid = resource.value)
    return mapOf(
      "self" to application.href(LapRoutes.LapId.Telemetry(parent = lap)),
      "lap" to application.href(lap),
    )
  }
}
