package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

const val MAX_VARCHAR_LENGTH = 255

object LapTable : LongIdTable("LAP") {
  val uid = varchar("uid", MAX_VARCHAR_LENGTH)
  val sessionId = long("session_id")
  val sessionUid = varchar("session_uid", MAX_VARCHAR_LENGTH)
  val recordedAt = timestamp("recorded_at")
  val lapTime = long("lap_time")
  val lapNumber = integer("lap_number")
  val valid = bool("valid")
  val personalBest = bool("personal_best")
}
