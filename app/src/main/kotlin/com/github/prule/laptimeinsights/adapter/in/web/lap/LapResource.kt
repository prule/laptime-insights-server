package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.enabledFeatures
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SessionRoutes
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable

@Serializable
data class LapResource(
  val uid: Uid,
  val sessionUid: Uid,
  val carId: CarId,
  val car: String?,
  val track: String?,
  val playerLap: Boolean?,
  val recordedAt: String,
  val lapTime: LapTimeMs,
  val lapNumber: LapNumber,
  val valid: ValidLap,
  val personalBest: PersonalBest,
  val _links: Map<String, String>,
) {

  companion object {
    fun fromDomain(lap: Lap, linkFactory: LapLinkFactory): LapResource =
      LapResource(
        uid = lap.uid,
        sessionUid = lap.sessionUId,
        carId = lap.carId,
        car = lap.car?.value,
        track = lap.track?.value,
        playerLap = lap.playerLap,
        recordedAt = lap.recordedAt.toString(),
        lapTime = lap.lapTime,
        lapNumber = lap.lapNumber,
        valid = lap.valid,
        personalBest = lap.personalBest,
        _links = linkFactory.build(lap),
      )
  }
}

class LapLinkFactory(private val application: Application) : LinkFactory<Lap> {
  override fun build(resource: Lap): Map<String, String> {
    val lap = LapRoutes.LapId(uid = resource.uid.value)
    val features = application.enabledFeatures()
    val links = linkedMapOf<String, String>("self" to application.href(lap))
    if (Feature.SESSIONS in features) {
      links["session"] = application.href(SessionRoutes.SessionId(uid = resource.sessionUId.value))
    }
    // Telemetry is part of the `laps` feature, so its rel is on the same gate as `self`. It only
    // gets its own toggle if telemetry ever becomes a separate feature.
    links["telemetry"] = application.href(LapRoutes.LapId.Telemetry(parent = lap))
    return links
  }
}
