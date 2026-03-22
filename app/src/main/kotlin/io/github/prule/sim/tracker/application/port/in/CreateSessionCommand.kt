package io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Car
import io.github.prule.sim.tracker.application.domain.model.LapId
import io.github.prule.sim.tracker.application.domain.model.LapNumber
import io.github.prule.sim.tracker.application.domain.model.LapTimeMs
import io.github.prule.sim.tracker.application.domain.model.PersonalBest
import io.github.prule.sim.tracker.application.domain.model.SessionId
import io.github.prule.sim.tracker.application.domain.model.SessionType
import io.github.prule.sim.tracker.application.domain.model.Simulator
import io.github.prule.sim.tracker.application.domain.model.Track
import io.github.prule.sim.tracker.application.domain.model.ValidLap
import kotlin.time.Instant

data class CreateSessionCommand(
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
)
