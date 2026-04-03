package com.github.prule.sim.tracker.application.port.`in`.session

import com.github.prule.sim.tracker.application.domain.model.Session

interface FinishSessionUseCase {
  fun finishSession(command: FinishSessionCommand): Session
}
