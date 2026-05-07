package com.github.prule.laptimeinsights.adapter.out.persistence.car

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.LongEntity
import org.jetbrains.exposed.v1.dao.LongEntityClass

class RealtimeCarUpdateEntity(id: EntityID<Long>) : LongEntity(id) {
  companion object : LongEntityClass<RealtimeCarUpdateEntity>(RealtimeCarUpdateTable)

  var sessionId by RealtimeCarUpdateTable.sessionId
  var sessionUid by RealtimeCarUpdateTable.sessionUid
  var lapId by RealtimeCarUpdateTable.lapId
  var lapUid by RealtimeCarUpdateTable.lapUid
  var recordedAt by RealtimeCarUpdateTable.recordedAt
  var carIndex by RealtimeCarUpdateTable.carIndex
  var driverIndex by RealtimeCarUpdateTable.driverIndex
  var driverCount by RealtimeCarUpdateTable.driverCount
  var gear by RealtimeCarUpdateTable.gear
  var worldPosX by RealtimeCarUpdateTable.worldPosX
  var worldPosY by RealtimeCarUpdateTable.worldPosY
  var yaw by RealtimeCarUpdateTable.yaw
  var carLocation by RealtimeCarUpdateTable.carLocation
  var kmh by RealtimeCarUpdateTable.kmh
  var racePosition by RealtimeCarUpdateTable.racePosition
  var cupPosition by RealtimeCarUpdateTable.cupPosition
  var trackPosition by RealtimeCarUpdateTable.trackPosition
  var splinePosition by RealtimeCarUpdateTable.splinePosition
  var laps by RealtimeCarUpdateTable.laps
  var delta by RealtimeCarUpdateTable.delta
  var bestLapTimeMs by RealtimeCarUpdateTable.bestLapTimeMs
  var lastLapTimeMs by RealtimeCarUpdateTable.lastLapTimeMs
  var currentLapTimeMs by RealtimeCarUpdateTable.currentLapTimeMs
  var currentLapIsInvalid by RealtimeCarUpdateTable.currentLapIsInvalid
  var currentLapIsOutlap by RealtimeCarUpdateTable.currentLapIsOutlap
  var currentLapIsInlap by RealtimeCarUpdateTable.currentLapIsInlap
}
