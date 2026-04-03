package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort

interface SearchLapUseCase {
  fun searchLaps(
      criteria: LapSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Lap>
}
