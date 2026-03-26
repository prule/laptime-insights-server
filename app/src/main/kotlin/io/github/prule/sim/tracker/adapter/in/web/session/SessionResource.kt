package io.github.prule.sim.tracker.adapter.`in`.web.session

import io.github.prule.sim.tracker.adapter.`in`.web.LinkFactory
import io.github.prule.sim.tracker.application.domain.model.Car
import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionType
import io.github.prule.sim.tracker.application.domain.model.Track
import io.github.prule.sim.tracker.application.domain.model.Uid
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class SessionResource(
    val uid: Uid,
    val startedAt: Instant?,
    val endedAt: Instant?,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
    val _links: Map<String, String>,
) {

  companion object {
    fun fromDomain(session: Session, linkFactory: SessionLinkFactory): SessionResource =
        SessionResource(
            uid = session.uid,
            startedAt = session.startedAt,
            endedAt = session.endedAt,
            track = session.track,
            car = session.car,
            sessionType = session.sessionType,
            _links = linkFactory.build(session),
        )
  }
}

class SessionLinkFactory(private val application: Application) : LinkFactory<Session> {
  override fun build(resource: Session): Map<String, String> {
    return mapOf("self" to application.href(SessionRoutes.SessionId(uid = resource.uid.value)))
  }
}
