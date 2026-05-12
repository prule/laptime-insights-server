package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateBucket
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.SessionOptions
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.port.out.session.AggregateSessionsPort
import com.github.prule.laptimeinsights.application.port.out.session.CreateSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import com.github.prule.laptimeinsights.tracker.utils.data.Page
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort

class SessionPersistenceAdapter(
  private val repository: SessionRepository,
  private val mapper: SessionMapper,
) : SearchSessionPort, CreateSessionPort, UpdateSessionPort, AggregateSessionsPort {
  override fun search(
    criteria: SessionSearchCriteria,
    pageRequest: PageRequest,
    sort: Sort,
  ): Page<Session> {
    return repository.search(criteria, pageRequest, sort).map(mapper::toDomain)
  }

  override fun searchForOne(criteria: SessionSearchCriteria, sort: Sort): Session? {
    return repository.searchForOne(criteria, sort)?.let(mapper::toDomain)
  }

  override fun options(criteria: SessionSearchCriteria): SessionOptions {
    return repository.options(criteria)
  }

  override fun create(session: Session): Session {
    val entity = repository.create(session)
    return mapper.toDomain(entity)
  }

  override fun update(session: Session): Session {
    val entity = repository.update(session)
    return mapper.toDomain(entity)
  }

  override fun aggregate(
    criteria: SessionSearchCriteria,
    groupBy: SessionAggregateGroupBy,
  ): List<SessionAggregateBucket> = repository.aggregate(criteria, groupBy)
}
