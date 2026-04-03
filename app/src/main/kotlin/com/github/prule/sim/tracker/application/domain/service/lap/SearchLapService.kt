package com.github.prule.sim.tracker.application.domain.service.lap

import com.github.prule.sim.tracker.application.domain.model.Lap
import com.github.prule.sim.tracker.application.domain.model.LapSearchCriteria
import com.github.prule.sim.tracker.application.port.`in`.lap.SearchLapUseCase
import com.github.prule.sim.tracker.application.port.out.lap.SearchLapPort
import com.github.prule.sim.tracker.utils.data.Page
import com.github.prule.sim.tracker.utils.data.PageRequest
import com.github.prule.sim.tracker.utils.data.Sort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SearchLapService(
    private val searchLapPort: SearchLapPort,
) : SearchLapUseCase {
  override fun searchLaps(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Lap> = transaction { searchLapPort.search(criteria, pageRequest, sort) }
}
