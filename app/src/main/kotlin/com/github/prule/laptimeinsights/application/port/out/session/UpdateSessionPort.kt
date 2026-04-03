package com.github.prule.laptimeinsights.application.port.out.session

import com.github.prule.laptimeinsights.application.domain.model.Session

interface UpdateSessionPort {
  fun update(session: Session): Session
}
