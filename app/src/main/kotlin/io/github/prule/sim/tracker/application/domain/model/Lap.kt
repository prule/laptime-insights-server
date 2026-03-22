package io.github.prule.sim.tracker.application.domain.model

import kotlin.time.Instant

@JvmInline
value class LapId(
    val value: Long,
)

@JvmInline
value class LapTimeMs(
    val value: Long,
)

@JvmInline
value class LapNumber(
    val value: Int,
)

@JvmInline
value class ValidLap(
    val value: Boolean,
)

@JvmInline
value class PersonalBest(
    val value: Boolean,
)

data class Lap(
    val id: LapId,
    val sessionId: SessionId,
    val recordedAt: Instant,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
