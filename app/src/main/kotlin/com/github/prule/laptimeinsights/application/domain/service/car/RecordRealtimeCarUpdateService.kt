package com.github.prule.laptimeinsights.application.domain.service.car

import com.github.prule.laptimeinsights.application.domain.model.PlayerCarUpdated
import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.port.`in`.car.RecordRealtimeCarUpdateCommand
import com.github.prule.laptimeinsights.application.port.`in`.car.RecordRealtimeCarUpdateUseCase
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.car.CreateRealtimeCarUpdatePort

/**
 * Records each REALTIME_CAR_UPDATE frame from ACC and, every [emitEveryN] invocations for the
 * player's own car, emits a lightweight [PlayerCarUpdated] domain event so the Live screen can
 * refresh without polling.
 *
 * ACC broadcasts at ~20 Hz per car; [emitEveryN] = 2 throttles the WS stream to ~10 Hz which keeps
 * the Live HUD responsive without saturating the WebSocket.
 */
class RecordRealtimeCarUpdateService(
  private val createPort: CreateRealtimeCarUpdatePort,
  private val eventPort: EventPort,
  private val emitEveryN: Int = 2,
) : RecordRealtimeCarUpdateUseCase {

  private var playerUpdateCounter = 0

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

    if (command.isPlayerCar) {
      playerUpdateCounter++
      if (playerUpdateCounter % emitEveryN == 0) {
        eventPort.emit(
          PlayerCarUpdated(
            sessionUid = command.sessionUid,
            gear = command.gear,
            kmh = command.kmh,
            splinePosition = command.splinePosition,
            worldPosX = command.worldPosX,
            worldPosY = command.worldPosY,
            racePosition = command.racePosition,
            currentLapTimeMs = command.currentLapTimeMs,
            currentLapIsInvalid = command.currentLapIsInvalid,
            delta = command.delta,
            bestLapTimeMs = command.bestLapTimeMs,
            lastLapTimeMs = command.lastLapTimeMs,
          )
        )
      }
    }
  }
}
