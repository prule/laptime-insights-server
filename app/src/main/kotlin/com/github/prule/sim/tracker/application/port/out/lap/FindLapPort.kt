package com.github.prule.sim.tracker.application.port.out.lap

import com.github.prule.sim.tracker.application.domain.model.Lap
import com.github.prule.sim.tracker.application.domain.model.LapId

interface FindLapPort {
  fun findById(id: LapId): Lap
}
