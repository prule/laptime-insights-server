package com.github.prule.sim.tracker.application.domain.model

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
    private var startedAt: Instant?,
    private var finishedAt: Instant?,
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
) {
  fun startedAt() = startedAt

  fun finishedAt() = finishedAt

  fun start(time: Instant) {
    if (canStart()) {
      startedAt = time
    }
  }

  fun finish(time: Instant) {
    if (canFinish()) {
      finishedAt = time
    }
  }

  fun isStarted(): Boolean {
    return startedAt != null
  }

  fun isFinished(): Boolean {
    return finishedAt != null
  }

  fun canStart(): Boolean {
    return !isStarted()
  }

  fun canFinish(): Boolean {
    return isStarted() && !isFinished()
  }
}
