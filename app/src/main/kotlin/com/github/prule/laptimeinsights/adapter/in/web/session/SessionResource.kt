package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.enabledFeatures
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapRoutes
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class SessionResource(
  val uid: Uid,
  val startedAt: Instant?,
  val simulator: Simulator,
  val track: Track?,
  val car: Car?,
  val sessionType: SessionType,
  /** ACC car index of the player's own car. Null until the EntryListCar message arrives. */
  val playerCarId: Int?,
  /** Cumulative time the player spent on track in this session, in milliseconds. */
  val drivingTimeMs: Long,
  val _links: Map<String, String>,
) {

  companion object {
    fun fromDomain(session: Session, linkFactory: SessionLinkFactory): SessionResource =
      SessionResource(
        uid = session.uid,
        startedAt = session.startedAt(),
        simulator = session.simulator,
        track = session.track,
        car = session.car,
        sessionType = session.sessionType,
        playerCarId = session.playerCarId?.value,
        drivingTimeMs = session.drivingTime().value,
        _links = linkFactory.build(session),
      )
  }
}

class SessionLinkFactory(private val application: Application) : LinkFactory<Session> {
  override fun build(resource: Session): Map<String, String> {
    val session = SessionRoutes.SessionId(uid = resource.uid.value)
    val features = application.enabledFeatures()
    val links = linkedMapOf<String, String>("self" to application.href(session))
    // Cross-feature rels are omitted when the target feature is off, so the frontend can
    // gate per-record actions purely by link presence instead of consulting the global toggle.
    if (Feature.LAPS in features) {
      links["laps"] = application.href(LapRoutes()) + "?sessionUid=${resource.uid.value}"
    }
    return links
  }
}
