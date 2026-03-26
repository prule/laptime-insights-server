package io.github.prule.sim.tracker.application.port.`in`.lap

import io.github.prule.sim.tracker.application.domain.model.LapNumber
import io.github.prule.sim.tracker.application.domain.model.LapTimeMs
import io.github.prule.sim.tracker.application.domain.model.PersonalBest
import io.github.prule.sim.tracker.application.domain.model.SessionId
import io.github.prule.sim.tracker.application.domain.model.ValidLap
import kotlin.time.Instant

data class CreateLapCommand(
    val sessionId: SessionId,
    val recordedAt: Instant,
    val lapTime: LapTimeMs,
    val lapNumber: LapNumber,
    val valid: ValidLap,
    val personalBest: PersonalBest,
)
