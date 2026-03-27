package io.github.prule.sim.tracker.application.port.out.lap

import io.github.prule.sim.tracker.application.domain.model.Lap

interface UpdateLapPort {
  fun update(lap: Lap): Lap
}
