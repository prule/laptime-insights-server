package com.github.prule.laptimeinsights.application.domain.model

sealed interface DomainEvent

data class SessionCreated(val session: Session) : DomainEvent

data class SessionStarted(val session: Session) : DomainEvent

data class SessionEnded(val session: Session) : DomainEvent

data class LapCreated(val lap: Lap) : DomainEvent

data class SessionUpdated(val session: Session) : DomainEvent

/**
 * Emitted on every Nth
 * [com.github.prule.laptimeinsights.application.port.in.car.RecordRealtimeCarUpdateCommand] where
 * [isPlayerCar] is true. Carries the lightweight telemetry fields needed by the Live screen. Heavy
 * fields (worldPosX/Y, racePosition, etc.) are included so the frontend can render the track-map
 * dot and HUD without a separate HTTP fetch.
 */
data class PlayerCarUpdated(
  val sessionUid: Uid,
  val gear: Int,
  val kmh: Int,
  val splinePosition: Double,
  val worldPosX: Float,
  val worldPosY: Float,
  val racePosition: Int,
  val currentLapTimeMs: Long,
  val currentLapIsInvalid: Boolean,
  val delta: Int,
  val bestLapTimeMs: Long,
  val lastLapTimeMs: Long,
) : DomainEvent
