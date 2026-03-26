package io.github.prule.sim.tracker

import io.github.prule.acc.client.app.io.github.prule.sim.tracker.adapter.`in`.web.FindSessionController
import io.github.prule.acc.client.app.io.github.prule.sim.tracker.application.domain.service.FindSessionService
import io.github.prule.sim.tracker.adapter.`in`.web.CreateSessionController
import io.github.prule.sim.tracker.adapter.`in`.web.SearchSessionController
import io.github.prule.sim.tracker.adapter.`in`.web.StartSessionController
import io.github.prule.sim.tracker.adapter.out.persistence.SessionMapper
import io.github.prule.sim.tracker.adapter.out.persistence.SessionPersistenceAdapter
import io.github.prule.sim.tracker.adapter.out.persistence.SessionRepository
import io.github.prule.sim.tracker.application.domain.service.CreateSessionService
import io.github.prule.sim.tracker.application.domain.service.SearchSessionService
import io.github.prule.sim.tracker.application.domain.service.StartSessionService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.resources.*
import kotlinx.serialization.json.Json
import kotlin.time.Clock

fun main() {
  println(Clock.System.now())
  embeddedServer(
          factory = Netty,
          port = 8000,
      ) {
        module()
      }
      .start(wait = true)
}

fun Application.module() {
  install(Resources)
  //    install(DefaultHeaders)
  //    install(CallLogging)

  install(ContentNegotiation) {
    json(
        Json { encodeDefaults = true },
    )
  }

  DatabaseFactory.init()

  val mapper = SessionMapper()
  val sessionPort = SessionPersistenceAdapter(SessionRepository(mapper), mapper)

  FindSessionController(this, FindSessionService(sessionPort))
  StartSessionController(this, StartSessionService(sessionPort, sessionPort))
  CreateSessionController(this, CreateSessionService(sessionPort))
  SearchSessionController(
      this,
      SearchSessionService(sessionPort),
  )
}
