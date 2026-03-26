package io.github.prule.sim.tracker.adapter.out.persistence.session

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.out.session.CreateSessionPort
import io.github.prule.sim.tracker.application.port.out.session.SearchSessionPort
import io.github.prule.sim.tracker.application.port.out.session.UpdateSessionPort
import io.github.prule.sim.tracker.utils.data.Page
import io.github.prule.sim.tracker.utils.data.PageRequest
import io.github.prule.sim.tracker.utils.data.Sort

class SessionPersistenceAdapter(
    private val repository: SessionRepository,
    private val mapper: SessionMapper,
) : SearchSessionPort, CreateSessionPort, UpdateSessionPort {
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

  override fun create(session: Session): Session {
    val entity = repository.create(session)
    return mapper.toDomain(entity)
  }

  override fun update(session: Session): Session {
    val entity = repository.update(session)
    return mapper.toDomain(entity)
  }
}
