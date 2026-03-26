package io.github.prule.sim.tracker.application.port.`in`.session

import io.github.prule.sim.tracker.application.domain.model.Car
import io.github.prule.sim.tracker.application.domain.model.SessionType
import io.github.prule.sim.tracker.application.domain.model.Simulator
import io.github.prule.sim.tracker.application.domain.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionCommand(
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
)
