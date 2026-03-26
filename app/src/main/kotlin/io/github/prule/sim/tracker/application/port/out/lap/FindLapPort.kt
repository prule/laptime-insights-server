package io.github.prule.sim.tracker.application.port.out.lap

import io.github.prule.sim.tracker.application.domain.model.Lap
import io.github.prule.sim.tracker.application.domain.model.LapId

interface FindLapPort {
  fun findById(id: LapId): Lap
}
