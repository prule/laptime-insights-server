package com.github.prule.laptimeinsights.application.port.out.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap

interface CreateLapPort {
  fun create(lap: Lap): Lap
}
