package com.github.prule.laptimeinsights.application.domain.service.car

import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.port.`in`.car.RecordRealtimeCarUpdateCommand
import com.github.prule.laptimeinsights.application.port.`in`.car.RecordRealtimeCarUpdateUseCase
import com.github.prule.laptimeinsights.application.port.out.car.CreateRealtimeCarUpdatePort

class RecordRealtimeCarUpdateService(private val createPort: CreateRealtimeCarUpdatePort) :
  RecordRealtimeCarUpdateUseCase {

  override fun record(command: RecordRealtimeCarUpdateCommand) {
    val update =
      RealtimeCarUpdate(
        sessionId = command.sessionId,
        sessionUid = command.sessionUid,
        lapId = command.lapId,
        lapUid = command.lapUid,
        recordedAt = command.recordedAt,
        carIndex = command.carIndex,
        driverIndex = command.driverIndex,
        driverCount = command.driverCount,
        gear = command.gear,
        worldPosX = command.worldPosX,
        worldPosY = command.worldPosY,
        yaw = command.yaw,
        carLocation = command.carLocation,
        kmh = command.kmh,
        racePosition = command.racePosition,
        cupPosition = command.cupPosition,
        trackPosition = command.trackPosition,
        splinePosition = command.splinePosition,
        laps = command.laps,
        delta = command.delta,
        bestLapTimeMs = command.bestLapTimeMs,
        lastLapTimeMs = command.lastLapTimeMs,
        currentLapTimeMs = command.currentLapTimeMs,
        currentLapIsInvalid = command.currentLapIsInvalid,
        currentLapIsOutlap = command.currentLapIsOutlap,
        currentLapIsInlap = command.currentLapIsInlap,
      )
    createPort.create(update)
  }
}
