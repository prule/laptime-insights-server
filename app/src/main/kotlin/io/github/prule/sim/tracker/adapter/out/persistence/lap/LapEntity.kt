package io.github.prule.sim.tracker.adapter.out.persistence.lap

import io.github.prule.sim.tracker.utils.data.SortableFields
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class LapEntity(
    id: EntityID<Long>,
) : LongEntity(id) {
  companion object : LongEntityClass<LapEntity>(LapTable) {
    val sortableFields =
        SortableFields(
            mapOf(
                "lapNumber" to LapTable.lapNumber,
                "lapTime" to LapTable.lapTime,
                "valid" to LapTable.valid,
            )
        )
  }

  var uid by LapTable.uid
  var sessionId by LapTable.sessionId
  var sessionUid by LapTable.sessionUid

  var recordedAt by LapTable.recordedAt
  var lapTime by LapTable.lapTime
  var lapNumber by LapTable.lapNumber
  var valid by LapTable.valid
  var personalBest by LapTable.personalBest
}
