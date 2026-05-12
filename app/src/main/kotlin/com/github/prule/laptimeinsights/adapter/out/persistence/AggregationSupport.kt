package com.github.prule.laptimeinsights.adapter.out.persistence

import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.stringLiteral
import org.jetbrains.exposed.v1.datetime.KotlinInstantColumnType

/** Time-bucket dimensions supported by the aggregation endpoints. */
enum class TimeBucketUnit(val sqlUnit: String) {
  DAY("DAY"),
  WEEK("WEEK"),
  MONTH("MONTH"),
}

/**
 * SQL `DATE_TRUNC(<unit>, <expr>)` wrapped as an Exposed expression. Supported by H2 and
 * PostgreSQL with matching semantics for `DAY`, `WEEK` (Monday start) and `MONTH`. The result is
 * typed as `Instant` so it can be selected into a `ResultRow` and read back via the same column
 * type the source column uses.
 */
fun dateTrunc(unit: TimeBucketUnit, expr: Expression<*>): Expression<Instant> =
  CustomFunction("DATE_TRUNC", KotlinInstantColumnType(), stringLiteral(unit.sqlUnit), expr)

private val MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM")

/**
 * Format a `DATE_TRUNC`-produced `Instant` into a stable wire key (`YYYY-MM-DD` for day/week,
 * `YYYY-MM` for month).
 *
 * Uses `ZoneId.systemDefault()` on purpose: Exposed's `InstantColumnType` stores `Instant`s as
 * TZ-less wall-clock values via `TimeZone.currentSystemDefault()` and reads them back the same
 * way, so the only way to recover the truncated wall-clock day is to format against the same
 * zone. In a single-user setup the JVM and browser TZ coincide, so the client sees the day the
 * player perceived the event on.
 */
fun formatTimeBucketKey(instant: Instant, unit: TimeBucketUnit): String {
  val localDate = instant.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()
  return if (unit == TimeBucketUnit.MONTH) localDate.format(MONTH_FORMAT) else localDate.toString()
}
