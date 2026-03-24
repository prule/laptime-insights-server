package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.out.CreateSessionPort
import io.github.prule.sim.tracker.application.port.out.SearchSessionPort
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort

class SessionPersistenceAdapter(
    private val repository: SessionRepository,
    private val mapper: SessionMapper,
) : SearchSessionPort, CreateSessionPort {
  override fun search(
      criteria: SessionSearchCriteria,
      pageRequest: PageRequest,
      sort: Sort,
  ): Page<Session> {
    return repository.search(criteria, pageRequest, sort).map(mapper::toDomain)
  }

  override fun create(session: Session): Session {
    val entity = repository.create(session)
    return mapper.toDomain(entity)
  }
}
