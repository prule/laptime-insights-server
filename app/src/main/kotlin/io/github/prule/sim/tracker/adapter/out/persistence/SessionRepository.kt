package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.utils.data.FindByCriteriaRepository
import io.github.prule.sim.tracker.utils.data.FindByIdRepository

class SessionRepository(
    private val mapper: SessionMapper,
) : FindByIdRepository<SessionEntity, Long>,
    FindByCriteriaRepository<SessionEntity> {
    override fun findOneOrNull(id: Long): SessionEntity? = SessionEntity.findById(id)

    fun create(session: Session): SessionEntity =
        SessionEntity.new {
            mapper.toEntity(session, this)
        }
}
