package com.github.prule.laptimeinsights.adapter.out.persistence.car

import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate
import com.github.prule.laptimeinsights.application.domain.model.TelemetrySample
import com.github.prule.laptimeinsights.application.domain.model.Uid
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll

/**
 * Persistence for REALTIME_CAR_UPDATE.
 *
 * Live updates arrive at ~100 ms intervals per car and use [create]. [batchCreate] is used by the
 * database seeder to efficiently insert many rows within a single transaction.
 *
 * [findByLapUid] projects the stored rows into [TelemetrySample] — the minimal shape needed by the
 * lap telemetry and lap comparison use cases. Results are ordered by
 * [RealtimeCarUpdateTable.splinePosition] ascending.
 */
class RealtimeCarUpdateRepository {

  fun create(update: RealtimeCarUpdate) {
    RealtimeCarUpdateTable.insert { it.applyFrom(update) }
  }

  fun batchCreate(updates: List<RealtimeCarUpdate>) {
    if (updates.isEmpty()) return
    RealtimeCarUpdateTable.batchInsert(updates) { update -> applyFrom(update) }
  }

  fun findByLapUid(lapUid: Uid): List<TelemetrySample> =
    RealtimeCarUpdateTable.selectAll()
      .where { RealtimeCarUpdateTable.lapUid eq lapUid.value }
      .orderBy(RealtimeCarUpdateTable.splinePosition, SortOrder.ASC)
      .map { row ->
        TelemetrySample(
          splinePosition = row[RealtimeCarUpdateTable.splinePosition],
          speedKph = row[RealtimeCarUpdateTable.kmh].toDouble(),
          gear = row[RealtimeCarUpdateTable.gear],
        )
      }
}

private fun UpdateBuilder<*>.applyFrom(update: RealtimeCarUpdate) {
  this[RealtimeCarUpdateTable.sessionId] = update.sessionId.value
  this[RealtimeCarUpdateTable.sessionUid] = update.sessionUid.value
  this[RealtimeCarUpdateTable.lapId] = update.lapId?.value
  this[RealtimeCarUpdateTable.lapUid] = update.lapUid?.value
  this[RealtimeCarUpdateTable.recordedAt] = update.recordedAt
  this[RealtimeCarUpdateTable.carIndex] = update.carIndex.value
  this[RealtimeCarUpdateTable.driverIndex] = update.driverIndex
  this[RealtimeCarUpdateTable.driverCount] = update.driverCount
  this[RealtimeCarUpdateTable.gear] = update.gear
  this[RealtimeCarUpdateTable.worldPosX] = update.worldPosX
  this[RealtimeCarUpdateTable.worldPosY] = update.worldPosY
  this[RealtimeCarUpdateTable.yaw] = update.yaw
  this[RealtimeCarUpdateTable.carLocation] = update.carLocation
  this[RealtimeCarUpdateTable.kmh] = update.kmh
  this[RealtimeCarUpdateTable.racePosition] = update.racePosition
  this[RealtimeCarUpdateTable.cupPosition] = update.cupPosition
  this[RealtimeCarUpdateTable.trackPosition] = update.trackPosition
  this[RealtimeCarUpdateTable.splinePosition] = update.splinePosition
  this[RealtimeCarUpdateTable.laps] = update.laps
  this[RealtimeCarUpdateTable.delta] = update.delta
  this[RealtimeCarUpdateTable.bestLapTimeMs] = update.bestLapTimeMs
  this[RealtimeCarUpdateTable.lastLapTimeMs] = update.lastLapTimeMs
  this[RealtimeCarUpdateTable.currentLapTimeMs] = update.currentLapTimeMs
  this[RealtimeCarUpdateTable.currentLapIsInvalid] = update.currentLapIsInvalid
  this[RealtimeCarUpdateTable.currentLapIsOutlap] = update.currentLapIsOutlap
  this[RealtimeCarUpdateTable.currentLapIsInlap] = update.currentLapIsInlap
}
