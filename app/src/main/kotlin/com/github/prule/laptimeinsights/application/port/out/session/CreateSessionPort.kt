package com.github.prule.laptimeinsights.application.port.out.session

import com.github.prule.laptimeinsights.application.domain.model.Session

interface CreateSessionPort {
  fun create(session: Session): Session
}
