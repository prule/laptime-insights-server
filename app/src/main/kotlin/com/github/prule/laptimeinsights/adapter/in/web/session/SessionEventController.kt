package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapLinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapResource
import com.github.prule.laptimeinsights.application.domain.model.LapCreated
import com.github.prule.laptimeinsights.application.domain.model.PlayerCarUpdated
import com.github.prule.laptimeinsights.application.domain.model.SessionCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionFinished
import com.github.prule.laptimeinsights.application.domain.model.SessionStarted
import com.github.prule.laptimeinsights.application.domain.model.SessionUpdated
import com.github.prule.laptimeinsights.application.port.out.EventPort
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket

/**
 * Real-time event stream controller exposing **`ws://<host>:<port>/api/1/events`**.
 *
 * Clients connect via WebSocket and receive a stream of JSON-encoded [WebSocketMessage] frames for
 * the following domain events as they occur:
 * - [WebSocketMessage.ServerStarted] — sent immediately on connect; clients reset live state on receipt.
 * - [SessionCreated] → [WebSocketMessage.SessionCreated] carrying a [SessionResource].
 * - [SessionStarted] → [WebSocketMessage.SessionStarted] carrying a [SessionResource].
 * - [SessionUpdated] → [WebSocketMessage.SessionUpdated] carrying a [SessionResource].
 * - [SessionFinished] → [WebSocketMessage.SessionFinished] carrying a [SessionResource].
 * - [LapCreated] → [WebSocketMessage.LapCreated] carrying a [LapResource].
 * - [PlayerCarUpdated] → [WebSocketMessage.PlayerCarUpdated] carrying a [PlayerCarUpdateData] (~10 Hz).
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
        // Send immediately so the client knows this is a fresh server connection and can
        // reset any stale in-memory state it accumulated before the server restarted.
        sendSerialized<WebSocketMessage>(WebSocketMessage.ServerStarted)

        val sessionLinks = SessionLinkFactory(application)
        val lapLinks = LapLinkFactory(application)
        eventPort.events.collect { event ->
          val message: WebSocketMessage? =
            when (event) {
              is SessionCreated ->
                WebSocketMessage.SessionCreated(
                  SessionResource.fromDomain(event.session, sessionLinks)
                )
              is SessionStarted ->
                WebSocketMessage.SessionStarted(
                  SessionResource.fromDomain(event.session, sessionLinks)
                )
              is SessionUpdated ->
                WebSocketMessage.SessionUpdated(
                  SessionResource.fromDomain(event.session, sessionLinks)
                )
              is SessionFinished ->
                WebSocketMessage.SessionFinished(
                  SessionResource.fromDomain(event.session, sessionLinks)
                )
              is LapCreated ->
                WebSocketMessage.LapCreated(LapResource.fromDomain(event.lap, lapLinks))
              is PlayerCarUpdated ->
                WebSocketMessage.PlayerCarUpdated(
                  PlayerCarUpdateData(
                    sessionUid = event.sessionUid.value,
                    gear = event.gear,
                    kmh = event.kmh,
                    splinePosition = event.splinePosition,
                    worldPosX = event.worldPosX,
                    worldPosY = event.worldPosY,
                    racePosition = event.racePosition,
                    currentLapTimeMs = event.currentLapTimeMs,
                    currentLapIsInvalid = event.currentLapIsInvalid,
                    delta = event.delta,
                    bestLapTimeMs = event.bestLapTimeMs,
                    lastLapTimeMs = event.lastLapTimeMs,
                  )
                )
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
