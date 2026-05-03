package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Persistence for `LAP_TELEMETRY`. Telemetry is treated as a complete set per
 * lap — there is no "update one sample" operation. Inserts are batched so
 * seeding 200 samples × many laps stays cheap.
 */
class LapTelemetryRepository(private val mapper: LapTelemetryMapper) {

  fun create(lapId: LapId, lapUid: Uid, samples: List<TelemetrySample>) {
    if (samples.isEmpty()) return
    LapTelemetryTable.batchInsert(samples) { sample ->
      this[LapTelemetryTable.lapId] = lapId.value
      this[LapTelemetryTable.lapUid] = lapUid.value
      this[LapTelemetryTable.splinePosition] = sample.splinePosition
      this[LapTelemetryTable.speedKph] = sample.speedKph
      this[LapTelemetryTable.gear] = sample.gear
      this[LapTelemetryTable.throttle] = sample.throttle
      this[LapTelemetryTable.brake] = sample.brake
    }
  }

  fun findByLapUid(lapUid: Uid): List<TelemetrySample> {
    return LapTelemetryTable.selectAll()
      .where { LapTelemetryTable.lapUid eq lapUid.value }
      .orderBy(LapTelemetryTable.splinePosition, SortOrder.ASC)
      .map { LapTelemetryEntity.wrapRow(it) }
      .map(mapper::toDomain)
  }
}
