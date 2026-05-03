package com.github.prule.laptimeinsights.application.domain.model

import kotlinx.serialization.Serializable

/**
 * One telemetry frame captured during a lap.
 *
 * Indexed by [splinePosition] (0.0 → 1.0 around the track) so two laps on the
 * same track can be compared 1:1 in space, regardless of their lap-time
 * difference. See `docs/specs/lap-comparison.md`.
 *
 * Stored as one row per sample in the LAP_TELEMETRY table — see
 * `LapTelemetryTable`.
 */
@Serializable
data class TelemetrySample(
  /** 0.0 (start/finish line) → 1.0 (back to start/finish). */
  val splinePosition: Double,
  /** Vehicle speed in kilometres per hour. */
  val speedKph: Double,
  /** Gear index. 0 = neutral, -1 = reverse, 1..N = forward gears. */
  val gear: Int,
  /** Throttle pedal application 0.0 → 1.0. */
  val throttle: Double,
  /** Brake pedal application 0.0 → 1.0. */
  val brake: Double,
)
