package com.github.prule.sim.tracker.application.port.`in`.lap

import com.github.prule.sim.tracker.application.domain.model.Lap
import com.github.prule.sim.tracker.application.domain.model.LapSearchCriteria
import com.github.prule.sim.tracker.utils.data.Page
import com.github.prule.sim.tracker.utils.data.PageRequest
import com.github.prule.sim.tracker.utils.data.Sort

interface SearchLapUseCase {
  fun searchLaps(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Lap>
}
