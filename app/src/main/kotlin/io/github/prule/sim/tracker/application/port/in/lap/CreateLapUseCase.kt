package io.github.prule.sim.tracker.application.port.`in`.lap

import io.github.prule.sim.tracker.application.domain.model.Lap

interface CreateLapUseCase {
  fun createLap(command: CreateLapCommand): Lap
}
