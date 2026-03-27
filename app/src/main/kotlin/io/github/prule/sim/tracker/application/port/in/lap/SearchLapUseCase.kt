package io.github.prule.sim.tracker.application.port.`in`.lap

import io.github.prule.sim.tracker.application.domain.model.Lap
import io.github.prule.sim.tracker.application.domain.model.LapSearchCriteria
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort

interface SearchLapUseCase {
  fun searchLaps(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Lap>
}
