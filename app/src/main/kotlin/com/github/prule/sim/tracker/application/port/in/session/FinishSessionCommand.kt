package com.github.prule.sim.tracker.application.port.`in`.session

import com.github.prule.sim.tracker.application.domain.model.Uid
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class FinishSessionCommand(
    val uid: Uid,
    val finishedAt: Instant,
)
