package io.github.prule.sim.tracker

import io.github.prule.sim.tracker.adapter.`in`.web.lap.CreateLapController
import io.github.prule.sim.tracker.adapter.`in`.web.lap.SearchLapController
import io.github.prule.sim.tracker.adapter.`in`.web.session.CreateSessionController
import io.github.prule.sim.tracker.adapter.`in`.web.session.FindSessionController
import io.github.prule.sim.tracker.adapter.`in`.web.session.FinishSessionController
import io.github.prule.sim.tracker.adapter.`in`.web.session.SearchSessionController
import io.github.prule.sim.tracker.adapter.`in`.web.session.StartSessionController
import io.github.prule.sim.tracker.adapter.out.persistence.lap.LapMapper
import io.github.prule.sim.tracker.adapter.out.persistence.lap.LapPersistenceAdapter
import io.github.prule.sim.tracker.adapter.out.persistence.lap.LapRepository
import io.github.prule.sim.tracker.adapter.out.persistence.session.SessionMapper
import io.github.prule.sim.tracker.adapter.out.persistence.session.SessionPersistenceAdapter
import io.github.prule.sim.tracker.adapter.out.persistence.session.SessionRepository
import io.github.prule.sim.tracker.application.domain.service.lap.CreateLapService
import io.github.prule.sim.tracker.application.domain.service.lap.SearchLapService
import io.github.prule.sim.tracker.application.domain.service.session.CreateSessionService
import io.github.prule.sim.tracker.application.domain.service.session.FindSessionService
import io.github.prule.sim.tracker.application.domain.service.session.FinishSessionService
import io.github.prule.sim.tracker.application.domain.service.session.SearchSessionService
import io.github.prule.sim.tracker.application.domain.service.session.StartSessionService
import io.ktor.http.ContentType
import io.ktor.openapi.OpenApiInfo
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.resources.Resources
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import kotlinx.serialization.json.Json

fun main() {
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

  initializeSessionControllers(sessionPort)
  initializeLapControllers(sessionPort)

  routing {
    swaggerUI("/swaggerUI") {
      info = OpenApiInfo("My API", "1.0")
      source = OpenApiDocSource.Routing(ContentType.Application.Json) { routingRoot.descendants() }
    }

    openAPI(path = "openapi") {
      info = OpenApiInfo("My API", "1.0")
      source = OpenApiDocSource.Routing { routingRoot.descendants() }
    }
  }
}

private fun Application.initializeSessionControllers(sessionPort: SessionPersistenceAdapter) {

  FindSessionController(this, FindSessionService(sessionPort))
  StartSessionController(this, StartSessionService(sessionPort, sessionPort))
  CreateSessionController(this, CreateSessionService(sessionPort))
  SearchSessionController(
      this,
      SearchSessionService(sessionPort),
  )
  FinishSessionController(this, FinishSessionService(sessionPort, sessionPort))
}

private fun Application.initializeLapControllers(sessionPort: SessionPersistenceAdapter) {
  val mapper = LapMapper()
  val lapPort = LapPersistenceAdapter(LapRepository(mapper), mapper)

  CreateLapController(this, CreateLapService(lapPort, sessionPort))
  SearchLapController(this, SearchLapService(lapPort))
}
