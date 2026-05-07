package com.github.prule.laptimeinsights.adapter.out.persistence.session

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

const val MAX_VARCHAR_LENGTH = 255

object SessionTable : LongIdTable("SESSION") {
  val uid = varchar("uid", MAX_VARCHAR_LENGTH)
  val startedAt = timestamp("started_at").nullable()
  val simulator = varchar("simulator", MAX_VARCHAR_LENGTH)
  val track = varchar("track", MAX_VARCHAR_LENGTH).nullable()
  val car = varchar("car", MAX_VARCHAR_LENGTH).nullable()
  val sessionType = varchar("session_type", MAX_VARCHAR_LENGTH)
  val playerCarId = integer("player_car_id").nullable()
  /** Cumulative driving time for the player's car, in milliseconds. See [Session.drivingTime]. */
  val drivingTimeMs = long("driving_time_ms").default(0L)
}
