package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
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
  /** When the session finished, or null while it is still live / for legacy rows. */
  val endedAt: Instant?,
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
        endedAt = session.endedAt(),
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
    // `_links` advertises capability — the rels are always present even when the matching UI
    // feature is hidden, so screens that compose data across features (e.g. Overview) keep
    // working. UI gating happens client-side via `enabledFeatures`.
    return mapOf(
      "self" to application.href(session),
      "laps" to application.href(LapRoutes()) + "?sessionUid=${resource.uid.value}",
    )
  }
}
