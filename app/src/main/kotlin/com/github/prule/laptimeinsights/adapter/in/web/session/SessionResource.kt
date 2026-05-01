package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
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
  val endedAt: Instant?,
  val simulator: Simulator,
  val track: Track?,
  val car: Car?,
  val sessionType: SessionType,
  val _links: Map<String, String>,
) {

  companion object {
    fun fromDomain(session: Session, linkFactory: SessionLinkFactory): SessionResource =
      SessionResource(
        uid = session.uid,
        startedAt = session.startedAt(),
        endedAt = session.finishedAt(),
        simulator = session.simulator,
        track = session.track,
        car = session.car,
        sessionType = session.sessionType,
        _links = linkFactory.build(session),
      )
  }
}

class SessionLinkFactory(private val application: Application) : LinkFactory<Session> {
  override fun build(resource: Session): Map<String, String> {
    val session = SessionRoutes.SessionId(uid = resource.uid.value)
    return listOfNotNull("self" to application.href(session)).toMap()
  }
}
