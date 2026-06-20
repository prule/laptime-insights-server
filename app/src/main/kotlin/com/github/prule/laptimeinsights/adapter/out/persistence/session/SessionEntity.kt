package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.tracker.utils.data.SortableFields
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class SessionEntity(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<SessionEntity>(SessionTable) {
    val sortableFields =
      SortableFields(
          mapOf(
            "startedAt" to SessionTable.startedAt,
            "track" to SessionTable.track,
            "car" to SessionTable.car,
            "sessionType" to SessionTable.sessionType,
            "simulator" to SessionTable.simulator,
            "drivingTimeMs" to SessionTable.drivingTimeMs,
          )
        )
        .also {
          require(it.mapping.keys == Session.SORTABLE_FIELDS.toSet()) {
            "SessionEntity.sortableFields keys ${it.mapping.keys} must match " +
              "Session.SORTABLE_FIELDS ${Session.SORTABLE_FIELDS}"
          }
        }
  }

  var uid by SessionTable.uid
  var startedAt by SessionTable.startedAt
  var endedAt by SessionTable.endedAt
  var simulator by SessionTable.simulator
  var track by SessionTable.track
  var car by SessionTable.car
  var sessionType by SessionTable.sessionType
  var playerCarId by SessionTable.playerCarId
  var drivingTimeMs by SessionTable.drivingTimeMs
}
