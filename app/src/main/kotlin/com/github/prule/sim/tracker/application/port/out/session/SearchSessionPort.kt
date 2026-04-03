package com.github.prule.sim.tracker.application.port.out.session

import com.github.prule.sim.tracker.application.domain.model.Session
import com.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import com.github.prule.sim.tracker.utils.data.Page
import com.github.prule.sim.tracker.utils.data.PageRequest
import com.github.prule.sim.tracker.utils.data.Sort

interface SearchSessionPort {
  fun search(
      criteria: SessionSearchCriteria,
      pageRequest: PageRequest = PageRequest(),
      sort: Sort = Sort.noSort(),
  ): Page<Session>

  fun searchForOne(criteria: SessionSearchCriteria, sort: Sort = Sort.noSort()): Session?
}
