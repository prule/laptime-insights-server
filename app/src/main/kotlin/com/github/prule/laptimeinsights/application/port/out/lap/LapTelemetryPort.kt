package com.github.prule.laptimeinsights.application.port.out.lap

import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid

/** Outbound port for persisting a complete telemetry trace for a lap. */
interface CreateLapTelemetryPort {
  fun create(lapId: LapId, lapUid: Uid, samples: List<TelemetrySample>)
}

/** Outbound port for retrieving the telemetry trace of a lap by its UID. */
interface FindLapTelemetryPort {
  fun findByLapUid(lapUid: Uid): List<TelemetrySample>
}
