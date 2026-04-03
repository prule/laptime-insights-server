package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap

class LapMapper {
  fun toDomain(entity: LapEntity): Lap =
      Lap(
          id = LapId(entity.id.value),
          uid = Uid(entity.uid),
          recordedAt = entity.recordedAt,
          lapTime = LapTimeMs(entity.lapTime),
          lapNumber = LapNumber(entity.lapNumber),
          valid = ValidLap(entity.valid),
          personalBest = PersonalBest(entity.personalBest),
          sessionId = SessionId(entity.sessionId),
          sessionUId = Uid(entity.sessionUid),
      )

  fun toEntity(
      lap: Lap,
      entity: LapEntity,
  ) {
    entity.apply {
      uid = lap.uid.value
      sessionId = lap.sessionId.value
      sessionUid = lap.sessionUId.value
      recordedAt = lap.recordedAt
      lapTime = lap.lapTime.value
      lapNumber = lap.lapNumber.value
      valid = lap.valid.value
      personalBest = lap.personalBest.value
    }
  }
}
