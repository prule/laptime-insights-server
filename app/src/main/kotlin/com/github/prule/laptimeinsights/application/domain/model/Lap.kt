package com.github.prule.laptimeinsights.application.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

@Serializable @JvmInline value class LapId(val value: Long)

@Serializable
@JvmInline
value class LapTimeMs(val value: Long) {
  companion object {
    fun fromString(time: String): LapTimeMs {
      return LapTimeMs(parseLapTime(time).toLong(DurationUnit.MILLISECONDS))
    }

    /** Parse duration like "01:36.745" */
    fun parseLapTime(input: String): Duration {
      val regex = Regex("""(\d+):(\d+)\.(\d+)""")
      val match = regex.find(input) ?: error("Invalid lap time: $input")
      val minutes = match.groupValues[1].toLong()
      val seconds = match.groupValues[2].toLong()
      val millis = match.groupValues[3].toLong()
      return (minutes * 60_000 + seconds * 1_000 + millis).milliseconds
    }
  }
}

@Serializable @JvmInline value class CarId(val value: Int)

@Serializable @JvmInline value class LapNumber(val value: Int)

@Serializable @JvmInline value class ValidLap(val value: Boolean)

@Serializable @JvmInline value class PersonalBest(val value: Boolean)

@Serializable
data class Lap(
    val id: LapId,
    val uid: Uid,
    val sessionId: SessionId,
    val sessionUId: Uid,
    val carId: CarId,
    val recordedAt: Instant,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
