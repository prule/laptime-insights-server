package io.github.prule.sim.tracker.application.domain.model

import kotlinx.serialization.Serializable
import java.util.*

@JvmInline
@Serializable
value class Uid(
    val value: String = UUID.randomUUID().toString().replace("-", ""),
)
