package io.github.prule.sim.tracker.application.port.`in`.session

import io.github.prule.sim.tracker.application.domain.model.Uid
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class FinishSessionCommand(
    val uid: Uid,
    val finishedAt: Instant,
)
