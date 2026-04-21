package com.github.prule.laptimeinsights.application.port.out

import com.github.prule.laptimeinsights.application.domain.model.DomainEvent
import kotlinx.coroutines.flow.SharedFlow

interface EventPort {
  suspend fun emit(event: DomainEvent)

  val events: SharedFlow<DomainEvent>
}
