package com.github.prule.laptimeinsights.adapter.out.persistence.car

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object RealtimeCarUpdateTable : LongIdTable("REALTIME_CAR_UPDATE") {
  val sessionId = long("session_id").index()
  val sessionUid = varchar("session_uid", 255).index()
  val lapId = long("lap_id").nullable().index()
  val lapUid = varchar("lap_uid", 255).nullable().index()
  val recordedAt = timestamp("recorded_at")
  val carIndex = integer("car_index").index()
  val driverIndex = integer("driver_index")
  val driverCount = integer("driver_count")
  val gear = integer("gear")
  val worldPosX = float("world_pos_x")
  val worldPosY = float("world_pos_y")
  val yaw = float("yaw")
  val carLocation = varchar("car_location", 32)
  val kmh = integer("kmh")
  val racePosition = integer("race_position")
  val cupPosition = integer("cup_position")
  val trackPosition = integer("track_position")
  val splinePosition = double("spline_position")
  val laps = integer("laps")
  val delta = integer("delta")
  val bestLapTimeMs = long("best_lap_time_ms")
  val lastLapTimeMs = long("last_lap_time_ms")
  val currentLapTimeMs = long("current_lap_time_ms")
  val currentLapIsInvalid = bool("current_lap_is_invalid")
  val currentLapIsOutlap = bool("current_lap_is_outlap")
  val currentLapIsInlap = bool("current_lap_is_inlap")
}
