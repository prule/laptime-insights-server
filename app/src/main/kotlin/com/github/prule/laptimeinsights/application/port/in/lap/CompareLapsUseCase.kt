package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid

/**
 * Use case: load two laps + their full telemetry traces so a client can render a side-by-side
 * comparison.
 *
 * Per the lap-comparison spec we return **raw** samples (no resampling) — the frontend aligns them
 * on `splinePosition` for display.
 */
interface CompareLapsUseCase {
  fun compare(lap1Uid: Uid, lap2Uid: Uid): LapComparison
}

data class LapComparison(
  val lap1: Lap,
  val lap1Samples: List<TelemetrySample>,
  val lap2: Lap,
  val lap2Samples: List<TelemetrySample>,
)
