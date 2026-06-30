package com.github.prule.laptimeinsights.application.domain.model

import kotlin.time.Instant

/**
 * Raw aggregates read from the local Session/Lap data, used to build the public profile snapshot.
 * Computed over the **player's** laps only. Presentation and identity are layered on top by the
 * snapshot service; this is purely the numbers the database can supply.
 */
data class ProfileAggregates(
  val playerLapCount: Long,
  val sessionCount: Long,
  /** Cumulative player driving time across all sessions, in milliseconds. */
  val seatTimeMs: Long,
  val trackCount: Int,
  val carCount: Int,
  /** Most-driven car this profile (by player lap count), or null when there are no laps. */
  val topCar: String?,
  /** Player laps per track, descending. */
  val perTrackLaps: List<TrackLaps>,
  /** Player laps per active calendar day (UTC-wall-clock `YYYY-MM-DD` key), ascending by date. */
  val activeDays: List<DayLaps>,
  /** Best valid player lap per (track, car): season (current year) + all-time, with its date. */
  val records: List<RecordAggregate>,
  val firstRecordedAt: Instant?,
  val lastRecordedAt: Instant?,
)

data class TrackLaps(val track: String, val laps: Long)

data class DayLaps(val dateKey: String, val laps: Long)

/**
 * Best lap for a (track, car) combination. [seasonBestMs] is the fastest valid player lap in the
 * current calendar year (null when none this year); [allTimeBestMs] is the fastest ever, recorded
 * at [allTimeWhen].
 */
data class RecordAggregate(
  val track: String,
  val car: String,
  val seasonBestMs: Long?,
  val allTimeBestMs: Long,
  val allTimeWhen: Instant,
)
