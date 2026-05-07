package com.github.prule.laptimeinsights.application.domain.model

import kotlin.time.Instant

/**
 * One frame of realtime telemetry for a single car, captured from a
 * [AccBroadcastingInbound.RealtimeCarUpdate] message.
 *
 * Received at ~100 ms intervals per car for every car on track. Persisted to REALTIME_CAR_UPDATE
 * for post-session analysis and replay.
 *
 * [lapId] / [lapUid] are nullable: at the time of recording the current lap is not yet complete, so
 * no Lap row exists yet. They remain null and can be correlated post-session via
 * [sessionId] + [carIndex] + [laps].
 */
data class RealtimeCarUpdate(
  val sessionId: SessionId,
  val sessionUid: Uid,
  /** Null until the lap completes and a Lap row is created. */
  val lapId: LapId?,
  val lapUid: Uid?,
  val recordedAt: Instant,
  val carIndex: CarId,
  val driverIndex: Int,
  val driverCount: Int,
  /** 0 = neutral, -1 = reverse, 1..N = forward gears. */
  val gear: Int,
  val worldPosX: Float,
  val worldPosY: Float,
  val yaw: Float,
  /** e.g. TRACK, PITLANE, PIT_ENTRY, PIT_EXIT. */
  val carLocation: String,
  val kmh: Int,
  /** Overall race/session position. */
  val racePosition: Int,
  val cupPosition: Int,
  val trackPosition: Int,
  /** 0.0 (start/finish) → 1.0 (back to start/finish). */
  val splinePosition: Double,
  /** Number of completed laps. */
  val laps: Int,
  /** Delta to best session lap in milliseconds. */
  val delta: Int,
  /** Best lap time this session in milliseconds. INT_MAX = no time set. */
  val bestLapTimeMs: Long,
  /** Most recently completed lap time in milliseconds. */
  val lastLapTimeMs: Long,
  /** Current (in-progress) lap time in milliseconds. */
  val currentLapTimeMs: Long,
  val currentLapIsInvalid: Boolean,
  val currentLapIsOutlap: Boolean,
  val currentLapIsInlap: Boolean,
)
