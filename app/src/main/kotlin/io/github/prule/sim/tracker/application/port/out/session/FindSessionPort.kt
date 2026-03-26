package io.github.prule.sim.tracker.application.port.out.session

import io.github.prule.sim.tracker.application.domain.model.Session
import io.github.prule.sim.tracker.application.domain.model.SessionId

interface FindSessionPort {
  fun findById(id: SessionId): Session
}
