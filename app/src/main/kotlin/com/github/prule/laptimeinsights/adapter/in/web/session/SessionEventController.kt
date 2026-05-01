package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapLinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapResource
import com.github.prule.laptimeinsights.application.domain.model.LapCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionCreated
import com.github.prule.laptimeinsights.application.port.out.EventPort
import io.ktor.server.application.Application
import io.ktor.server.routing.routing
import io.ktor.server.websocket.sendSerialized
import io.ktor.server.websocket.webSocket

/**
 * Real-time event stream controller exposing **`ws://<host>:<port>/api/1/events`**.
 *
 * Clients connect via WebSocket and receive a stream of JSON-encoded resources for selected
 * domain events as they occur:
 * - On [SessionCreated] — a [SessionResource] for the newly created session.
 * - On [LapCreated] — a [LapResource] for the newly recorded lap.
 *
 * Other domain events ([com.github.prule.laptimeinsights.application.domain.model.SessionUpdated],
 * [com.github.prule.laptimeinsights.application.domain.model.SessionFinished]) are not currently
 * forwarded to clients — they are silently dropped by the `else` branch below. Add a `when`
 * branch here if a new event type needs to be broadcast.
 *
 * Each frame is a single JSON object whose shape is identical to the corresponding REST resource
 * (including the `_links` HATEOAS field). There is no envelope and no explicit type
 * discriminator; clients distinguish messages by the resource shape (e.g. presence of `lapTime`
 * vs `simulator`). See `docs/real-time-updates.md` for the full protocol description.
 *
 * **OpenAPI note:** OpenAPI 3.x has no first-class WebSocket support, so this endpoint does not
 * appear in `/openapi` or `/swaggerUI`. The KDoc above plus `docs/real-time-updates.md` are the
 * canonical contract.
 */
class SessionEventController(application: Application, eventPort: EventPort) {
  init {
    application.routing {
      webSocket("/api/1/events") {
        eventPort.events.collect { event ->
          when (event) {
            is SessionCreated -> {
              sendSerialized(
                SessionResource.fromDomain(event.session, SessionLinkFactory(application))
              )
            }
            is LapCreated -> {
              sendSerialized(LapResource.fromDomain(event.lap, LapLinkFactory(application)))
            }
            else -> {}
          }
        }
      }
    }
  }
}
