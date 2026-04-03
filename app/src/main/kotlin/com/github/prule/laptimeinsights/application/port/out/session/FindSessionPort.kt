package com.github.prule.laptimeinsights.application.port.out.session

import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId

interface FindSessionPort {
  fun findById(id: SessionId): Session
}
