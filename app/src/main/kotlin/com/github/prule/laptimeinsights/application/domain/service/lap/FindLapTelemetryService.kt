package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapTelemetryUseCase
import com.github.prule.laptimeinsights.application.port.out.lap.FindLapTelemetryPort
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FindLapTelemetryService(
  private val searchLapPort: SearchLapPort,
  private val findLapTelemetryPort: FindLapTelemetryPort,
) : FindLapTelemetryUseCase {
  override fun findByLapUid(lapUid: Uid): List<TelemetrySample> = transaction {
    // Validate the lap exists; otherwise an empty array would be ambiguous.
    searchLapPort.searchForOne(LapSearchCriteria(uid = lapUid))
      ?: throw NotFoundException("Lap:${lapUid.value}")
    findLapTelemetryPort.findByLapUid(lapUid)
  }
}
