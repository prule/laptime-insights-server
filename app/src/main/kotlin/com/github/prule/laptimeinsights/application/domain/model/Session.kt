package com.github.prule.laptimeinsights.application.domain.model

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@JvmInline @Serializable value class SessionId(val value: Long)

@JvmInline @Serializable value class Track(val value: String)

@JvmInline @Serializable value class Car(val value: String)

@JvmInline @Serializable value class SessionType(val value: String)

@Serializable
enum class Simulator {
  ACC,
  F1,
}

/**
 * A single recorded session for a simulator + track + car.
 *
 * Session lifetime is bounded by [startedAt] only — sessions have no explicit "finished" timestamp.
 * Activity is instead derived from the laps recorded against the session: [drivingTime] is the
 * cumulative lap time for the *player's* car (identified by [playerCarId]) and grows as new laps
 * are recorded. Competitor lap times are recorded but do not contribute to [drivingTime] — it
 * reflects how long the player was actually on track.
 */
data class Session(
  val id: SessionId,
  val uid: Uid,
  private var startedAt: Instant?,
  val simulator: Simulator,
  val track: Track?,
  val car: Car?,
  val sessionType: SessionType,
  /** ACC car index of the player's own car. Null until the EntryListCar message arrives. */
  val playerCarId: CarId? = null,
  /**
   * Cumulative time the player spent on track in this session — sum of [lapTime][Lap.lapTime] for
   * every lap whose [Lap.carId] equals [playerCarId]. Maintained by `CreateLapService`; readers
   * should treat it as authoritative rather than re-summing laps.
   */
  private var drivingTime: LapTimeMs = LapTimeMs(0),
) {
  fun startedAt() = startedAt

  fun drivingTime() = drivingTime

  fun start(time: Instant) {
    if (canStart()) {
      startedAt = time
    } else {
      throw IllegalStateException("Session cannot be started")
    }
  }

  /**
   * Adds [lapTime] to the running [drivingTime] aggregate. Called by `CreateLapService` for every
   * persisted lap belonging to the player's car so the column stays in sync without a full re-sum.
   */
  fun addDriving(lapTime: LapTimeMs) {
    drivingTime = LapTimeMs(drivingTime.value + lapTime.value)
  }

  fun isStarted(): Boolean {
    return startedAt != null
  }

  fun canStart(): Boolean {
    return !isStarted()
  }

  companion object {
    /**
     * Field names a client may pass in the `sort` query parameter of `GET /api/1/sessions`. The
     * persistence adapter (`SessionEntity.sortableFields`) maps each name to its Exposed column;
     * the search controller surfaces this list on the page response so the UI knows which table
     * columns to render as sortable.
     */
    val SORTABLE_FIELDS: List<String> = listOf("car")
  }
}
