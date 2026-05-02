package com.github.prule.laptimeinsights.application.port.`in`.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap

interface FindLapUseCase {
  fun findLap(command: FindLapCommand): Lap
}
