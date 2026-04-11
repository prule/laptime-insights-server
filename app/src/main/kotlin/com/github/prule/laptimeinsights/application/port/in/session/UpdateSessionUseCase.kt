package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Session

interface UpdateSessionUseCase {
  fun update(command: UpdateSessionCommand): Session
}
