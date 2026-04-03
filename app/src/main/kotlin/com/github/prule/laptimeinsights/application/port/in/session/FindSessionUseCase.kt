package com.github.prule.laptimeinsights.application.port.`in`.session

import com.github.prule.laptimeinsights.application.domain.model.Session

interface FindSessionUseCase {
  fun findSession(command: FindSessionCommand): Session
}
