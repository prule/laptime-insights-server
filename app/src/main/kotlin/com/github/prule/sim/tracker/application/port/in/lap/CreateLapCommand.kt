package com.github.prule.sim.tracker.application.port.`in`.lap

import com.github.prule.sim.tracker.application.domain.model.LapNumber
import com.github.prule.sim.tracker.application.domain.model.LapTimeMs
import com.github.prule.sim.tracker.application.domain.model.PersonalBest
import com.github.prule.sim.tracker.application.domain.model.Uid
import com.github.prule.sim.tracker.application.domain.model.ValidLap
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CreateLapCommand(
    val sessionUid: Uid,
    val recordedAt: Instant,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
