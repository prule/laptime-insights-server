package com.github.prule.laptimeinsights.application.port.out.session

import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionOptions
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort

interface SearchSessionPort {
  fun search(
    criteria: SessionSearchCriteria,
    pageRequest: PageRequest = PageRequest(),
    sort: Sort = Sort.noSort(),
  ): Page<Session>

  fun searchForOne(criteria: SessionSearchCriteria, sort: Sort = Sort.noSort()): Session?

  fun options(criteria: SessionSearchCriteria): SessionOptions
}
