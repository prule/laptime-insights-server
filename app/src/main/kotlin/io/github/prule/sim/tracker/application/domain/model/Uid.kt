package io.github.prule.sim.tracker.application.domain.model

import java.util.UUID

@JvmInline
value class Uid(
    val value: String = UUID.randomUUID().toString().replace("-", ""),
)
