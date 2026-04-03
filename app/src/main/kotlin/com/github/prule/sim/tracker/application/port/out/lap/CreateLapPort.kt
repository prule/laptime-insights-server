package com.github.prule.sim.tracker.application.port.out.lap

import com.github.prule.sim.tracker.application.domain.model.Lap

interface CreateLapPort {
  fun create(lap: Lap): Lap
}
