package com.github.prule.laptimeinsights.adapter.out.persistence.car

import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.Uid

class RealtimeCarUpdateMapper {
  fun toDomain(entity: RealtimeCarUpdateEntity): RealtimeCarUpdate =
    RealtimeCarUpdate(
      sessionId = SessionId(entity.sessionId),
      sessionUid = Uid(entity.sessionUid),
      lapId = entity.lapId?.let { LapId(it) },
      lapUid = entity.lapUid?.let { Uid(it) },
      recordedAt = entity.recordedAt,
      carIndex = CarId(entity.carIndex),
      driverIndex = entity.driverIndex,
      driverCount = entity.driverCount,
      gear = entity.gear,
      worldPosX = entity.worldPosX,
      worldPosY = entity.worldPosY,
      yaw = entity.yaw,
      carLocation = entity.carLocation,
      kmh = entity.kmh,
      racePosition = entity.racePosition,
      cupPosition = entity.cupPosition,
      trackPosition = entity.trackPosition,
      splinePosition = entity.splinePosition,
      laps = entity.laps,
      delta = entity.delta,
      bestLapTimeMs = entity.bestLapTimeMs,
      lastLapTimeMs = entity.lastLapTimeMs,
      currentLapTimeMs = entity.currentLapTimeMs,
      currentLapIsInvalid = entity.currentLapIsInvalid,
      currentLapIsOutlap = entity.currentLapIsOutlap,
      currentLapIsInlap = entity.currentLapIsInlap,
    )
}
