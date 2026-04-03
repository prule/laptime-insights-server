package com.github.prule.sim.tracker.application.port.out.session

import com.github.prule.sim.tracker.application.domain.model.Session

interface CreateSessionPort {
  fun create(session: Session): Session
}
