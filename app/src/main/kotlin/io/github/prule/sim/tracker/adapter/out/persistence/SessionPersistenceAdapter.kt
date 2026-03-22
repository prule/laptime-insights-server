package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionSearchCriteria
import io.github.prule.sim.tracker.application.port.out.CreateSessionPort
import io.github.prule.sim.tracker.application.port.out.SearchSessionPort
import io.github.prule.sim.tracker.utils.data.Page

class SessionPersistenceAdapter(
    private val repository: SessionRepository,
    private val mapper: SessionMapper,
) :
    // SearchSessionPort,
    CreateSessionPort {
//    override fun search(criteria: SessionSearchCriteria): Page<Session> {
//        repository.search()
//    }

    override fun create(session: Session) {
        repository.create {
            it[SessionTable.uid] = session.uid.value
            it[SessionTable.startedAt] = session.startedAt
            it[SessionTable.endedAt] = session.endedAt
            it[SessionTable.simulator] = session.simulator.name
            it[SessionTable.track] = session.track.value
            it[SessionTable.car] = session.car.value
            it[SessionTable.sessionType] = session.sessionType.value
        }
    }
}
