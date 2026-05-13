package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.LapAggregate
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria

interface AggregateLapsUseCase {
  /**
   * Returns the count of laps matching [criteria], grouped by [groupBy]. The result is sparse — the
   * client fills any zero-count buckets it needs for layout.
   */
  fun aggregate(criteria: LapSearchCriteria, groupBy: LapAggregateGroupBy): LapAggregate
}
