package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.tracker.utils.data.SortableFields
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class SessionEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
  companion object : LongEntityClass<SessionEntity>(SessionTable) {
    val sortableFields = SortableFields(mapOf("car" to SessionTable.car))
  }

  var uid by SessionTable.uid
  var startedAt by SessionTable.startedAt
  var finishedAt by SessionTable.finishedAt
  var simulator by SessionTable.simulator
  var track by SessionTable.track
  var car by SessionTable.car
  var sessionType by SessionTable.sessionType
}
