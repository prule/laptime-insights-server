package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

object LapTelemetryTable : LongIdTable("LAP_TELEMETRY") {
  val lapId = long("lap_id").index()
  val lapUid = varchar("lap_uid", MAX_VARCHAR_LENGTH).index()
  val splinePosition = double("spline_position")
  val speedKph = double("speed_kph")
  val gear = integer("gear")
  val throttle = double("throttle")
  val brake = double("brake")
}
