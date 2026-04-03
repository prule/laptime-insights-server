package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class StartSessionCommand(
    val uid: Uid,
    val startedAt: Instant,
)
