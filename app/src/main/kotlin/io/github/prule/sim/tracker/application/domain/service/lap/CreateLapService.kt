package io.github.prule.sim.tracker.application.domain.service.lap

import io.github.prule.sim.tracker.application.port.`in`.lap.CreateLapCommand
import io.github.prule.sim.tracker.application.port.`in`.lap.CreateLapUseCase
import io.github.prule.sim.tracker.application.port.out.lap.CreateLapPort

class CreateLapService(
    private val createLapPort: CreateLapPort,
) : CreateLapUseCase {
  override fun createLap(command: CreateLapCommand) {
    TODO("Not yet implemented")
  }
}
