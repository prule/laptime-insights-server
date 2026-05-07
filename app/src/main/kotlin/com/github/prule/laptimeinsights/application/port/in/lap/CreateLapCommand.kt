package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Inputs needed to record a freshly completed lap.
 *
 * Note that `personalBest` is intentionally absent — the PB flag is derived inside
 * [com.github.prule.laptimeinsights.application.domain.service.lap.CreateLapService]
 * by comparing the new lap against the existing PB for the same session+car. Callers
 * (the ACC ingest path, DB seeders, etc.) only need to report what telemetry observed:
 * the lap time and whether it was clean.
 */
@Serializable
data class CreateLapCommand(
  val sessionUid: Uid,
  val recordedAt: Instant,
  val carId: CarId,
  val car: Car?,
  val lapTime: LapTimeMs,
  val lapNumber: LapNumber,
  val valid: ValidLap,
)
