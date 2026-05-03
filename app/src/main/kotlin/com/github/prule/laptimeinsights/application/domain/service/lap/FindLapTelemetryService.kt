package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapTelemetryUseCase
import com.github.prule.laptimeinsights.application.port.out.car.FindRealtimeCarUpdateByLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FindLapTelemetryService(
  private val searchLapPort: SearchLapPort,
  private val findRealtimeCarUpdateByLapPort: FindRealtimeCarUpdateByLapPort,
) : FindLapTelemetryUseCase {
  override fun findByLapUid(lapUid: Uid): List<TelemetrySample> = transaction {
    // Validate the lap exists; otherwise an empty list would be ambiguous.
    searchLapPort.searchForOne(LapSearchCriteria(uid = lapUid))
      ?: throw NotFoundException("Lap:${lapUid.value}")
    findRealtimeCarUpdateByLapPort.findByLapUid(lapUid)
  }
}
