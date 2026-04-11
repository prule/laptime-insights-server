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

class SessionEventController(
    application: Application,
    eventPort: EventPort,
) {
  init {
    application.routing {
      webSocket("/api/1/events") {
        eventPort.events.collect { event ->
          when (event) {
            is SessionCreated -> {
              sendSerialized(
                  SessionResource.fromDomain(
                      event.session,
                      SessionLinkFactory(application),
                  )
              )
            }
            is LapCreated -> {
              sendSerialized(
                  LapResource.fromDomain(
                      event.lap,
                      LapLinkFactory(application)
                  )
              )
            }
            else -> {}
          }
        }
      }
    }
  }
}
