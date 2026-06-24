package com.github.prule.laptimeinsights.application.port.`in`.car

import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.Uid

/**
 * Inbound port invoked when a lap completes, to link the realtime telemetry frames captured during
 * that lap to the freshly created Lap row.
 */
interface LinkLapTelemetryUseCase {
  fun linkCompletedLap(
    sessionUid: Uid,
    carIndex: CarId,
    lapNumber: Int,
    lapId: LapId,
    lapUid: Uid,
  )
}
