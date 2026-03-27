package io.github.prule.sim.tracker.application.port.out.lap

import io.github.prule.sim.tracker.application.domain.model.Lap
import io.github.prule.sim.tracker.application.domain.model.LapSearchCriteria
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort

interface SearchLapPort {
  fun search(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest = PageRequest(),
      sort: Sort = Sort.noSort(),
  ): Page<Lap>

  fun searchForOne(criteria: LapSearchCriteria, sort: Sort = Sort.noSort()): Lap?
}
