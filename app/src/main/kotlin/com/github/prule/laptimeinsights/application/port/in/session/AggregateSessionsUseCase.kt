package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.SessionAggregate
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria

interface AggregateSessionsUseCase {
  fun aggregate(criteria: SessionSearchCriteria, groupBy: SessionAggregateGroupBy): SessionAggregate
}
