package io.github.prule.sim.tracker.application.port.out.session

import io.github.prule.sim.tracker.application.domain.model.Session

interface CreateSessionPort {
  fun create(session: Session): Session
}
