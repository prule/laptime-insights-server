package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapTelemetryPort
import com.github.prule.laptimeinsights.application.port.out.lap.FindLapTelemetryPort

class LapTelemetryPersistenceAdapter(private val repository: LapTelemetryRepository) :
  CreateLapTelemetryPort, FindLapTelemetryPort {

  override fun create(lapId: LapId, lapUid: Uid, samples: List<TelemetrySample>) {
    repository.create(lapId, lapUid, samples)
  }

  override fun findByLapUid(lapUid: Uid): List<TelemetrySample> {
    return repository.findByLapUid(lapUid)
  }
}
