package com.github.prule.sim.tracker.adapter.out.persistence.session

import com.github.prule.sim.tracker.application.domain.model.Car
import com.github.prule.sim.tracker.application.domain.model.Session
import com.github.prule.sim.tracker.application.domain.model.SessionId
import com.github.prule.sim.tracker.application.domain.model.SessionType
import com.github.prule.sim.tracker.application.domain.model.Simulator
import com.github.prule.sim.tracker.application.domain.model.Track
import com.github.prule.sim.tracker.application.domain.model.Uid

class SessionMapper {
  fun toDomain(entity: SessionEntity): Session =
      Session(
          id = SessionId(entity.id.value),
          uid = Uid(entity.uid),
          startedAt = entity.startedAt,
          finishedAt = entity.finishedAt,
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
      startedAt = session.startedAt()
      finishedAt = session.finishedAt()
      simulator = session.simulator.name
      track = session.track.value
      car = session.car.value
      sessionType = session.sessionType.value
    }
  }
}
