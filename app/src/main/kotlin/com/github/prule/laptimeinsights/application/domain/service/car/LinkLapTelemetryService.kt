package com.github.prule.laptimeinsights.application.domain.service.car

import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.car.LinkLapTelemetryUseCase
import com.github.prule.laptimeinsights.application.port.out.car.LinkLapTelemetryPort

/**
 * Links the realtime telemetry frames captured during a lap to the Lap row created when that lap
 * completes. Without this step the frames keep the null `lapUid` they were recorded with, and the
 * lap telemetry / comparison queries (which look up by `lapUid`) return nothing.
 */
class LinkLapTelemetryService(private val linkPort: LinkLapTelemetryPort) :
  LinkLapTelemetryUseCase {
  override fun linkCompletedLap(
    sessionUid: Uid,
    carIndex: CarId,
    lapNumber: Int,
    lapId: LapId,
    lapUid: Uid,
  ) {
    linkPort.linkFramesToLap(sessionUid, carIndex, lapNumber, lapId, lapUid)
  }
}
