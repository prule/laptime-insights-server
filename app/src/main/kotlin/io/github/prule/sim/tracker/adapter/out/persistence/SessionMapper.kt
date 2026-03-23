package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session

class SessionMapper {
    fun toDomain(entity: SessionEntity): Session =
        Session(
            id = entity.id.value,
            startedAt = entity.startedAt,
            endedAt = entity.endedAt,
            simulator = entity.simulator,
            track = entity.track,
            car = entity.car,
            sessionType = entity.sessionType,
        )

    fun toEntity(
        session: Session,
        entity: SessionEntity,
    ) {
        entity.apply {
            startedAt = session.startedAt
            endedAt = session.endedAt
            simulator = session.simulator.name
            track = session.track.value
            car = session.car.value
            sessionType = session.sessionType.value
        }
    }
}
