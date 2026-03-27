package io.github.prule.sim.tracker.application.domain.service.lap

import io.github.prule.sim.tracker.application.domain.model.Lap
import io.github.prule.sim.tracker.application.domain.model.LapSearchCriteria
import io.github.prule.sim.tracker.application.port.`in`.lap.SearchLapUseCase
import io.github.prule.sim.tracker.application.port.out.lap.SearchLapPort
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort
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
