package com.github.prule.sim.tracker.application.port.`in`.session

import com.github.prule.sim.tracker.application.domain.model.Session

interface FindSessionUseCase {
  fun findSession(command: FindSessionCommand): Session
}
