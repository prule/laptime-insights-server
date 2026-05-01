package com.github.prule.laptimeinsights.adapter.out.event

import com.github.prule.laptimeinsights.application.domain.model.DomainEvent
import com.github.prule.laptimeinsights.application.port.out.EventPort
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory broadcaster backed by a [MutableSharedFlow].
 *
 * The flow is configured with an effectively unbounded buffer (`extraBufferCapacity =
 * Int.MAX_VALUE`) and `BufferOverflow.SUSPEND`, but because the buffer is never expected to fill in
 * practice [tryEmit] is non-blocking and always succeeds.
 *
 * If the buffer ever does fill (e.g. all subscribers disappear and emissions are unbounded)
 * `tryEmit` returns `false` and we log a warning rather than blocking the caller. This is the right
 * trade-off for our use case: dropping an event for an absent subscriber is preferable to stalling
 * a database transaction.
 */
class InMemoryEventAdapter : EventPort {
  private val logger = org.slf4j.LoggerFactory.getLogger(javaClass)
  private val _events =
    MutableSharedFlow<DomainEvent>(
      extraBufferCapacity = Int.MAX_VALUE,
      onBufferOverflow = BufferOverflow.SUSPEND,
    )
  override val events = _events.asSharedFlow()

  override fun emit(event: DomainEvent) {
    logger.debug("Event emitted : {}", event)
    if (!_events.tryEmit(event)) {
      logger.warn("Dropping event because the in-memory buffer is full: {}", event)
    }
  }
}
