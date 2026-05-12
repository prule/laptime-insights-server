package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.SessionAggregate
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.`in`.session.AggregateSessionsUseCase
import com.github.prule.laptimeinsights.application.port.out.session.AggregateSessionsPort
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class AggregateSessionsService(private val aggregateSessionsPort: AggregateSessionsPort) :
  AggregateSessionsUseCase {
  override fun aggregate(
    criteria: SessionSearchCriteria,
    groupBy: SessionAggregateGroupBy,
  ): SessionAggregate = transaction {
    SessionAggregate(groupBy, aggregateSessionsPort.aggregate(criteria, groupBy))
  }
}
