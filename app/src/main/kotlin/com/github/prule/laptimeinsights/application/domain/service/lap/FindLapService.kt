package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.lap.FindLapUseCase
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class FindLapService(private val searchLapPort: SearchLapPort) : FindLapUseCase {
  override fun findLap(command: FindLapCommand): Lap = transaction {
    searchLapPort.searchForOne(LapSearchCriteria(uid = command.uid))
      ?: throw NotFoundException(command.uid.toString())
  }
}
