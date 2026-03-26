package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Uid
import kotlinx.serialization.Serializable

@Serializable
data class FindSessionCommand(
    val uid: Uid,
)
