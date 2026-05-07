package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.lap.CompareLapsUseCase
import com.github.prule.laptimeinsights.application.port.`in`.lap.LapComparison
import com.github.prule.laptimeinsights.application.port.out.car.FindRealtimeCarUpdateByLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class CompareLapsService(
  private val searchLapPort: SearchLapPort,
  private val findRealtimeCarUpdateByLapPort: FindRealtimeCarUpdateByLapPort,
) : CompareLapsUseCase {
  override fun compare(lap1Uid: Uid, lap2Uid: Uid): LapComparison = transaction {
    val lap1 =
      searchLapPort.searchForOne(LapSearchCriteria(uid = lap1Uid))
        ?: throw NotFoundException("Lap:${lap1Uid.value}")
    val lap2 =
      searchLapPort.searchForOne(LapSearchCriteria(uid = lap2Uid))
        ?: throw NotFoundException("Lap:${lap2Uid.value}")
    LapComparison(
      lap1 = lap1,
      lap1Samples = findRealtimeCarUpdateByLapPort.findByLapUid(lap1Uid),
      lap2 = lap2,
      lap2Samples = findRealtimeCarUpdateByLapPort.findByLapUid(lap2Uid),
    )
  }
}
