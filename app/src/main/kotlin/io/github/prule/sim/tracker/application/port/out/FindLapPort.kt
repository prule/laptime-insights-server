package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.out

import io.github.prule.sim.tracker.application.domain.model.Lap
import io.github.prule.sim.tracker.application.domain.model.LapId

interface FindLapPort {
    fun findById(id: LapId): Lap
}
