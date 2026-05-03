package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.LapComparison
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable

/**
 * Wire format for `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…`.
 *
 * Per the lap-comparison spec we return raw, un-resampled samples for each
 * lap (the frontend aligns on `splinePosition` for display). Each side carries
 * a `LapComparisonSide` so callers can render lap meta (lap time, lap number,
 * UID) without a follow-up call to `/api/1/laps/{uid}`.
 */
@Serializable
data class LapComparisonResource(
  val lap1: LapComparisonSide,
  val lap2: LapComparisonSide,
  val _links: Map<String, String>,
) {
  companion object {
    fun fromDomain(
      comparison: LapComparison,
      linkFactory: LapComparisonLinkFactory,
    ): LapComparisonResource =
      LapComparisonResource(
        lap1 = LapComparisonSide.fromDomain(comparison.lap1, comparison.lap1Samples),
        lap2 = LapComparisonSide.fromDomain(comparison.lap2, comparison.lap2Samples),
        _links = linkFactory.build(comparison),
      )
  }
}

@Serializable
data class LapComparisonSide(
  val lapUid: Uid,
  val sessionUid: Uid,
  val lapNumber: Int,
  /** Lap time in milliseconds. */
  val lapTimeMs: Long,
  val valid: Boolean,
  val personalBest: Boolean,
  val samples: List<TelemetrySample>,
) {
  companion object {
    fun fromDomain(lap: Lap, samples: List<TelemetrySample>): LapComparisonSide =
      LapComparisonSide(
        lapUid = lap.uid,
        sessionUid = lap.sessionUId,
        lapNumber = lap.lapNumber.value,
        lapTimeMs = lap.lapTime.value,
        valid = lap.valid.value,
        personalBest = lap.personalBest.value,
        samples = samples,
      )
  }
}

class LapComparisonLinkFactory(private val application: Application) : LinkFactory<LapComparison> {
  override fun build(resource: LapComparison): Map<String, String> {
    return mapOf(
      "self" to application.href(LapRoutes.Compare()) +
        "?lap1Uid=${resource.lap1.uid.value}&lap2Uid=${resource.lap2.uid.value}",
      "lap1" to application.href(LapRoutes.LapId(uid = resource.lap1.uid.value)),
      "lap2" to application.href(LapRoutes.LapId(uid = resource.lap2.uid.value)),
    )
  }
}
