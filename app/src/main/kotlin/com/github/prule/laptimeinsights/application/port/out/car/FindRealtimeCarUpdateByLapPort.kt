package com.github.prule.laptimeinsights.application.port.out.car

import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid

/**
 * Outbound port for retrieving the telemetry trace of a lap from REALTIME_CAR_UPDATE, projected as
 * [TelemetrySample] for use in the lap telemetry and lap comparison use cases.
 *
 * Results are ordered by [TelemetrySample.splinePosition] ascending.
 */
interface FindRealtimeCarUpdateByLapPort {
  fun findByLapUid(lapUid: Uid): List<TelemetrySample>
}
