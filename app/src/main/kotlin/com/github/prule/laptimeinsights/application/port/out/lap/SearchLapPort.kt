package com.github.prule.laptimeinsights.application.port.out.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort

interface SearchLapPort {
  fun search(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest = PageRequest(),
      sort: Sort = Sort.noSort(),
  ): Page<Lap>

  fun searchForOne(criteria: LapSearchCriteria, sort: Sort = Sort.noSort()): Lap?
}
