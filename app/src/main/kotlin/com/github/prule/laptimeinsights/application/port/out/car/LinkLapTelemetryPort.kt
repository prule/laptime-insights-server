package com.github.prule.laptimeinsights.application.port.out.car

import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.Uid

/**
 * Outbound port for back-filling the lap reference onto the realtime telemetry frames that were
 * recorded while a lap was in progress.
 *
 * Frames are persisted to REALTIME_CAR_UPDATE with a null [lapId]/[lapUid] because the owning Lap
 * row does not exist yet at capture time. Once a lap completes and its Lap row is created, this port
 * correlates the still-unlinked frames for that car via `sessionUid + carIndex + laps`, so the lap
 * telemetry and comparison queries (which look up by `lapUid`) can find them.
 */
interface LinkLapTelemetryPort {
  /**
   * Stamp [lapId]/[lapUid] onto every still-unlinked frame for [carIndex] in [sessionUid] whose
   * `laps` counter is below [lapNumber] (i.e. frames captured during this lap or an earlier one that
   * was never linked). Returns the number of frames linked.
   */
  fun linkFramesToLap(
    sessionUid: Uid,
    carIndex: CarId,
    lapNumber: Int,
    lapId: LapId,
    lapUid: Uid,
  ): Int
}
