package io.github.prule.sim.tracker.application.port.`in`.lap

interface CreateLapUseCase {
  fun createLap(command: CreateLapCommand)
}
