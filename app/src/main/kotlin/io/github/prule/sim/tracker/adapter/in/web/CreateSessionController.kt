package io.github.prule.sim.tracker.adapter.`in`.web

import io.ktor.http.ContentType
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

class CreateSessionController(
    application: Application,
) {
    init {
        application.routing {
            post("/api/1/session") {
                // call.respondText("hello", contentType = ContentType.Text.Plain)
            }
        }
    }
}
