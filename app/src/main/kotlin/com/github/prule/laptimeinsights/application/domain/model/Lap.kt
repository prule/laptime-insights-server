package com.github.prule.laptimeinsights.application.domain.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
@JvmInline
value class LapId(
    val value: Long,
)

@Serializable
@JvmInline
value class LapTimeMs(
    val value: Long,
)

@Serializable
@JvmInline
value class LapNumber(
    val value: Int,
)

@Serializable
@JvmInline
value class ValidLap(
    val value: Boolean,
)

@Serializable
@JvmInline
value class PersonalBest(
    val value: Boolean,
)

@Serializable
data class Lap(
    val id: LapId,
    val uid: Uid,
    val sessionId: SessionId,
    val sessionUId: Uid,
    val recordedAt: Instant,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
