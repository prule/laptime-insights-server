package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid

/** Use case: get every telemetry sample recorded during a single lap. */
interface FindLapTelemetryUseCase {
  fun findByLapUid(lapUid: Uid): List<TelemetrySample>
}
