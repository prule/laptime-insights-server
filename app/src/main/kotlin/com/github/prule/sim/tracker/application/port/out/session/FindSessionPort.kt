package com.github.prule.sim.tracker.application.port.out.session

import com.github.prule.sim.tracker.application.domain.model.Session
import com.github.prule.sim.tracker.application.domain.model.SessionId

interface FindSessionPort {
  fun findById(id: SessionId): Session
}
