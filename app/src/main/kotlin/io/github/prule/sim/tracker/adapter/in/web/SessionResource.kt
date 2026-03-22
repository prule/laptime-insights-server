package io.github.prule.sim.tracker.adapter.`in`.web

import io.github.prule.sim.tracker.application.domain.model.Car
import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionType
import io.github.prule.sim.tracker.application.domain.model.Track
import io.github.prule.sim.tracker.application.domain.model.Uid
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
) {
    companion object {
        fun fromDomain(session: Session): SessionResource =
            SessionResource(
                uid = session.uid,
                startedAt = session.startedAt,
                endedAt = session.endedAt,
                track = session.track,
                car = session.car,
                sessionType = session.sessionType,
            )
    }
}
