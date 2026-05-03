package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class LapTelemetryEntity(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<LapTelemetryEntity>(LapTelemetryTable)

  var lapId by LapTelemetryTable.lapId
  var lapUid by LapTelemetryTable.lapUid
  var splinePosition by LapTelemetryTable.splinePosition
  var speedKph by LapTelemetryTable.speedKph
  var gear by LapTelemetryTable.gear
  var throttle by LapTelemetryTable.throttle
  var brake by LapTelemetryTable.brake
}
