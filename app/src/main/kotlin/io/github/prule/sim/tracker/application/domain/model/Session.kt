package io.github.prule.sim.tracker.application.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@JvmInline
@Serializable
value class SessionId(
    val value: Long,
)

@JvmInline
@Serializable
value class Track(
    val value: String,
)

@JvmInline
@Serializable
value class Car(
    val value: String,
)

@JvmInline
@Serializable
value class SessionType(
    val value: String,
)

@Serializable
enum class Simulator {
  ACC,
  F1,
}

data class Session(
    val id: SessionId,
    val uid: Uid,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
) {
  fun canStart(): Boolean {
    return startedAt == null
  }

  fun canFinish(): Boolean {
    return startedAt != null && finishedAt == null
  }
}
