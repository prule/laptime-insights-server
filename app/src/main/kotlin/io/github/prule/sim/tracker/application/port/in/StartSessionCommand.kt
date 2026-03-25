package io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Uid
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class StartSessionCommand(
    val uid: Uid,
    val startedAt: Instant,
)
