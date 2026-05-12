package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.LapAggregate
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.lap.AggregateLapsUseCase
import com.github.prule.laptimeinsights.application.port.out.lap.AggregateLapsPort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AggregateLapsService(private val aggregateLapsPort: AggregateLapsPort) :
  AggregateLapsUseCase {
  override fun aggregate(criteria: LapSearchCriteria, groupBy: LapAggregateGroupBy): LapAggregate =
    transaction {
      LapAggregate(groupBy, aggregateLapsPort.aggregate(criteria, groupBy))
    }
}
