package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.port.out.CreateSessionPort

class SessionPersistenceAdapter(
    private val repository: SessionRepository,
    private val mapper: SessionMapper,
) :
// SearchSessionPort,
    CreateSessionPort {
//    override fun search(criteria: SessionSearchCriteria): Page<Session> {
//        repository.search()
//    }

    override fun create(session: Session): Session {
        val entity = repository.create()
        return mapper.toDomain(entity)
    }
}
