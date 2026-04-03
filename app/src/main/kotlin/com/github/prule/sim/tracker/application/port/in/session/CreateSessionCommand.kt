package com.github.prule.sim.tracker.application.port.`in`.session

import com.github.prule.sim.tracker.application.domain.model.Car
import com.github.prule.sim.tracker.application.domain.model.SessionType
import com.github.prule.sim.tracker.application.domain.model.Simulator
import com.github.prule.sim.tracker.application.domain.model.Track
import kotlinx.serialization.Serializable

@Serializable
data class CreateSessionCommand(
    val simulator: Simulator,
    val track: Track,
    val car: Car,
    val sessionType: SessionType,
)
