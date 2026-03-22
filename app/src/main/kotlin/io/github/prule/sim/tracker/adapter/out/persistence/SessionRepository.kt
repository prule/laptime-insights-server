package io.github.prule.sim.tracker.adapter.out.persistence

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.utils.data.FindByCriteriaRepository
import io.github.prule.sim.tracker.utils.data.FindByIdRepository
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.jdbc.insert

class SessionRepository(
    private val mapper: SessionMapper,
) : FindByIdRepository<SessionEntity, Long>,
    FindByCriteriaRepository<SessionEntity> {
    override fun findOneOrNull(id: Long): SessionEntity? = SessionEntity.findById(id)

    fun create(body: SessionTable.(InsertStatement<Number>) -> Unit): Session = mapper.toDomain(SessionTable.insert(body))
}
