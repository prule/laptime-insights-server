package com.github.prule.laptimeinsights.application.port.`in`.car

import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlin.time.Instant

data class RecordRealtimeCarUpdateCommand(
  val sessionId: SessionId,
  val sessionUid: Uid,
  val lapId: LapId?,
  val lapUid: Uid?,
  val recordedAt: Instant,
  val carIndex: CarId,
  val driverIndex: Int,
  val driverCount: Int,
  val gear: Int,
  val worldPosX: Float,
  val worldPosY: Float,
  val yaw: Float,
  val carLocation: String,
  val kmh: Int,
  val racePosition: Int,
  val cupPosition: Int,
  val trackPosition: Int,
  val splinePosition: Double,
  val laps: Int,
  val delta: Int,
  val bestLapTimeMs: Long,
  val lastLapTimeMs: Long,
  val currentLapTimeMs: Long,
  val currentLapIsInvalid: Boolean,
  val currentLapIsOutlap: Boolean,
  val currentLapIsInlap: Boolean,
  /** True when this update is for the player's own car (focusedCarIndex). */
  val isPlayerCar: Boolean = false,
)
