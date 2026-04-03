package com.github.prule.laptimeinsights.application.port.out.lap

import com.github.prule.laptimeinsights.application.domain.model.Lap

interface UpdateLapPort {
  fun update(lap: Lap): Lap
}
