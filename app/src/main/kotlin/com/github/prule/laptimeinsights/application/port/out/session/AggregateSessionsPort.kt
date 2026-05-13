package com.github.prule.laptimeinsights.application.port.out.session

import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateBucket
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria

interface AggregateSessionsPort {
  fun aggregate(
    criteria: SessionSearchCriteria,
    groupBy: SessionAggregateGroupBy,
  ): List<SessionAggregateBucket>
}
