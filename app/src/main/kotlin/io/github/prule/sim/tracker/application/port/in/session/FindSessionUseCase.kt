package io.github.prule.sim.tracker.application.port.`in`.session

import io.github.prule.sim.tracker.application.domain.model.Session

interface FindSessionUseCase {
  fun findSession(command: FindSessionCommand): Session
}
