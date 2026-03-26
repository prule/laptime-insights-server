package io.github.prule.sim.tracker.adapter.out.persistence.session

import io.github.prule.sim.tracker.application.domain.model.Car
import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionId
import io.github.prule.sim.tracker.application.domain.model.SessionType
import io.github.prule.sim.tracker.application.domain.model.Simulator
import io.github.prule.sim.tracker.application.domain.model.Track
import io.github.prule.sim.tracker.application.domain.model.Uid

class SessionMapper {
  fun toDomain(entity: SessionEntity): Session =
      Session(
          id = SessionId(entity.id.value),
          uid = Uid(entity.uid),
          startedAt = entity.startedAt,
          endedAt = entity.endedAt,
          simulator = Simulator.valueOf(entity.simulator),
          track = Track(entity.track),
          car = Car(entity.car),
          sessionType = SessionType(entity.sessionType),
      )

  fun toEntity(
      session: Session,
      entity: SessionEntity,
  ) {
    entity.apply {
      uid = session.uid.value
      startedAt = session.startedAt
      endedAt = session.endedAt
      simulator = session.simulator.name
      track = session.track.value
      car = session.car.value
      sessionType = session.sessionType.value
    }
  }
}
