package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapLinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapResource
import com.github.prule.laptimeinsights.application.domain.model.LapCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionFinished
import com.github.prule.laptimeinsights.application.domain.model.SessionUpdated
import com.github.prule.laptimeinsights.application.port.out.EventPort
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket

/**
 * Real-time event stream controller exposing **`ws://<host>:<port>/api/1/events`**.
 *
 * Clients connect via WebSocket and receive a stream of JSON-encoded [WebSocketMessage] frames
 * for the following domain events as they occur:
 * - [SessionCreated]  → [WebSocketMessage.SessionCreated]  carrying a [SessionResource].
 * - [SessionUpdated]  → [WebSocketMessage.SessionUpdated]  carrying a [SessionResource].
 * - [SessionFinished] → [WebSocketMessage.SessionFinished] carrying a [SessionResource].
 * - [LapCreated]      → [WebSocketMessage.LapCreated]      carrying a [LapResource].
 *
 * Each frame is wrapped in a typed envelope of the form `{ "type": "...", "data": { ... } }` so
 * clients can dispatch by the `type` field rather than guessing from resource shape. Adding a new
 * forwarded event type means adding both a [WebSocketMessage] subclass and a `when` branch here.
 *
 * **OpenAPI note:** OpenAPI 3.x has no first-class WebSocket support, so this endpoint does not
 * appear in `/openapi` or `/swaggerUI`. The KDoc above plus `docs/real-time-updates.md` are the
 * canonical contract.
 */
class SessionEventController(application: Application, eventPort: EventPort) {
  init {
    application.routing {
      webSocket("/api/1/events") {
        val sessionLinks = SessionLinkFactory(application)
        val lapLinks = LapLinkFactory(application)
        eventPort.events.collect { event ->
          val message: WebSocketMessage? =
            when (event) {
              is SessionCreated ->
                WebSocketMessage.SessionCreated(SessionResource.fromDomain(event.session, sessionLinks))
              is SessionUpdated ->
                WebSocketMessage.SessionUpdated(SessionResource.fromDomain(event.session, sessionLinks))
              is SessionFinished ->
                WebSocketMessage.SessionFinished(SessionResource.fromDomain(event.session, sessionLinks))
              is LapCreated ->
                WebSocketMessage.LapCreated(LapResource.fromDomain(event.lap, lapLinks))
              else -> null
            }
          if (message != null) {
            sendSerialized<WebSocketMessage>(message)
          }
        }
      }
    }
  }
}
