package io.github.prule.sim.tracker.application.port.`in`.lap

import io.github.prule.sim.tracker.application.domain.model.LapNumber
import io.github.prule.sim.tracker.application.domain.model.LapTimeMs
import io.github.prule.sim.tracker.application.domain.model.PersonalBest
import io.github.prule.sim.tracker.application.domain.model.Uid
import io.github.prule.sim.tracker.application.domain.model.ValidLap
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class CreateLapCommand(
    val sessionUid: Uid,
    val recordedAt: Instant,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
