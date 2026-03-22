package io.github.prule.sim.tracker.application.domain.model

import kotlin.time.Instant

@JvmInline
value class SessionId(
    val value: Long,
)

@JvmInline
value class Track(
    val value: String,
)

@JvmInline
value class Car(
    val value: String,
)

@JvmInline
value class SessionType(
    val value: String,
)

enum class Simulator {
    ACC,
    F1,
}

data class Session(
    val id: SessionId,
    val startedAt: Instant,
    val endedAt: Instant?,
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
)
