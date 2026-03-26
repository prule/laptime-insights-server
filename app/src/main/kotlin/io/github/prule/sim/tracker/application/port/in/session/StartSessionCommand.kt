package io.github.prule.sim.tracker.application.port.`in`.session

import io.github.prule.sim.tracker.application.domain.model.Uid
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class StartSessionCommand(
    val uid: Uid,
    val startedAt: Instant,
)
