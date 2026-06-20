package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Session

interface EndSessionUseCase {
  fun endSession(command: EndSessionCommand): Session
}
