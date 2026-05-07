package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid

class SessionMapper {
  fun toDomain(entity: SessionEntity): Session =
    Session(
      id = SessionId(entity.id.value),
      uid = Uid(entity.uid),
      startedAt = entity.startedAt,
      simulator = Simulator.valueOf(entity.simulator),
      track = Track(entity.track ?: "Unknown"),
      car = Car(entity.car ?: "Unknown"),
      sessionType = SessionType(entity.sessionType),
      playerCarId = entity.playerCarId?.let { CarId(it) },
      drivingTime = LapTimeMs(entity.drivingTimeMs),
    )

  fun toEntity(session: Session, entity: SessionEntity) {
    entity.apply {
      uid = session.uid.value
      startedAt = session.startedAt()
      simulator = session.simulator.name
      session.track?.let { track = it.value }
      session.car?.let { car = it.value }
      sessionType = session.sessionType.value
      session.playerCarId?.let { playerCarId = it.value }
      drivingTimeMs = session.drivingTime().value
    }
  }
}
