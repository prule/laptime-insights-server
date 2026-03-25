package io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Session

interface StartSessionUseCase {
  fun startSession(command: StartSessionCommand): Session
}
