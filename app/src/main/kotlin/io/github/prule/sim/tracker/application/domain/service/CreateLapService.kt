package io.github.prule.sim.tracker.application.domain.service

import io.github.prule.sim.tracker.application.port.`in`.CreateLapCommand
import io.github.prule.sim.tracker.application.port.`in`.CreateLapUseCase
import io.github.prule.sim.tracker.application.port.out.CreateLapPort

class CreateLapService(
    private val createLapPort: CreateLapPort,
) : CreateLapUseCase {
    override fun createLap(command: CreateLapCommand) {
        TODO("Not yet implemented")
    }
}
