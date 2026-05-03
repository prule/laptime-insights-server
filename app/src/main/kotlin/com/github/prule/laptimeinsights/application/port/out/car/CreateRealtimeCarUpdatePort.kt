package com.github.prule.laptimeinsights.application.port.out.car

import com.github.prule.laptimeinsights.application.domain.model.RealtimeCarUpdate

interface CreateRealtimeCarUpdatePort {
  fun create(update: RealtimeCarUpdate)

  /** Batch insert — more efficient than repeated [create] calls (e.g. database seeding). */
  fun batchCreate(updates: List<RealtimeCarUpdate>)
}
