package io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.port.`in`

import io.github.prule.sim.tracker.application.domain.model.Session

interface FindSessionUseCase {
  fun findSession(command: FindSessionCommand): Session
}
