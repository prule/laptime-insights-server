package io.github.prule.sim.tracker.adapter.`in`.web.lap

import io.ktor.server.application.Application
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

class CreateLapController(
    application: Application,
) {
  init {
    application.routing {
      post("/api/1/lap") {
        // call.respondText("hello", contentType = ContentType.Text.Plain)
      }
    }
  }
}
