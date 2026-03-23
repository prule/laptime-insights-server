package io.github.prule.sim.tracker.adapter.out.persistence

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class SessionEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
    companion object : LongEntityClass<SessionEntity>(SessionTable)

    var uid by SessionTable.uid
    var startedAt by SessionTable.startedAt
    var endedAt by SessionTable.endedAt
    var simulator by SessionTable.simulator
    var track by SessionTable.track
    var car by SessionTable.car
    var sessionType by SessionTable.sessionType
}
