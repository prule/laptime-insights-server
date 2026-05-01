package com.github.prule.laptimeinsights.application.port.out

import com.github.prule.laptimeinsights.application.domain.model.DomainEvent
import kotlinx.coroutines.flow.SharedFlow

/**
 * Outbound port for broadcasting [DomainEvent]s to interested observers (e.g. the WebSocket
 * controller).
 *
 * `emit` is intentionally **non-suspending**: callers must be able to invoke it from inside
 * `transaction { }` blocks without holding the JDBC connection while waiting for slow subscribers.
 * Implementations should use a buffered, non-blocking pipeline (e.g. `MutableSharedFlow` with
 * `extraBufferCapacity` and `tryEmit`).
 */
interface EventPort {
  fun emit(event: DomainEvent)

  val events: SharedFlow<DomainEvent>
}
