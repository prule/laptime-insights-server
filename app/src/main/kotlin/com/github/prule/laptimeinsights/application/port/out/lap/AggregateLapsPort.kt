package com.github.prule.laptimeinsights.application.port.out.lap

import com.github.prule.laptimeinsights.application.domain.model.LapAggregateBucket
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria

interface AggregateLapsPort {
  fun aggregate(criteria: LapSearchCriteria, groupBy: LapAggregateGroupBy): List<LapAggregateBucket>
}
