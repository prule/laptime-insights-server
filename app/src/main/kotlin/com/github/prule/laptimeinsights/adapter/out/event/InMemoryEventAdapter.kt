package com.github.prule.laptimeinsights.adapter.out.event

import com.github.prule.laptimeinsights.application.domain.model.DomainEvent
import com.github.prule.laptimeinsights.application.port.out.EventPort
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class InMemoryEventAdapter : EventPort {
  private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
  private val _events = MutableSharedFlow<DomainEvent>()
  override val events = _events.asSharedFlow()

  override suspend fun emit(event: DomainEvent) {
    logger.debug("Event emitted : {}", event)
    _events.emit(event)
  }
}
