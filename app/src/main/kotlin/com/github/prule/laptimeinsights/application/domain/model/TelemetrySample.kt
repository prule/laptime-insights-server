package com.github.prule.laptimeinsights.application.domain.model

import kotlinx.serialization.Serializable

/**
 * A telemetry snapshot for a single point around the track.
 *
 * Indexed by [splinePosition] (0.0 → 1.0) so two laps on the same track can be compared 1:1 in
 * space. See `docs/specs/lap-comparison.md`.
 *
 * Projected from [RealtimeCarUpdate] rows stored in REALTIME_CAR_UPDATE. Throttle and brake are not
 * available from the ACC UDP broadcasting protocol.
 *
 * [worldPosX] and [worldPosY] are the ACC world-space coordinates (metres). They are used by the
 * frontend to render a 2-D track map overlay on the comparison screen.
 */
@Serializable
data class TelemetrySample(
  /** 0.0 (start/finish line) → 1.0 (back to start/finish). */
  val splinePosition: Double,
  /** Vehicle speed in kilometres per hour. */
  val speedKph: Double,
  /** Gear index. 0 = neutral, -1 = reverse, 1..N = forward gears. */
  val gear: Int,
  /** ACC world-space X coordinate in metres. */
  val worldPosX: Float,
  /** ACC world-space Y coordinate in metres (lateral / depth axis). */
  val worldPosY: Float,
)
