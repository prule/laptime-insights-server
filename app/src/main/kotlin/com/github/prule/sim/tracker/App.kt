package com.github.prule.sim.tracker

import com.github.prule.acc.client.AccClient
import com.github.prule.acc.client.AccClientConfiguration
import com.github.prule.acc.client.ClientState
import com.github.prule.acc.client.LoggingListener
import com.github.prule.acc.client.RegistrationResultListener
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
import kotlinx.coroutines.runBlocking
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

  com.github.prule.sim.tracker.DatabaseFactory.init()

  val mapper = com.github.prule.sim.tracker.adapter.out.persistence.session.SessionMapper()
  val sessionPort =
      com.github.prule.sim.tracker.adapter.out.persistence.session.SessionPersistenceAdapter(
          com.github.prule.sim.tracker.adapter.out.persistence.session.SessionRepository(mapper),
          mapper,
      )

  initializeSessionControllers(sessionPort)
  initializeLapControllers(sessionPort)

  initializeClient()

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

private fun initializeClient() = runBlocking {
  val clientState = ClientState()
  AccClient(
          AccClientConfiguration(
              "Test",
              port = 9000,
              serverIp = "127.0.0.1",
              //            serverIp = "192.168.86.116",
          ),
      )
      .connect(
          listOf(
              LoggingListener(),
              //              CsvWriterListener(
              //                  java.nio.file.Path.of("./recordings"),
              //              ),
              RegistrationResultListener(clientState),
          ),
      )
}

private fun Application.initializeSessionControllers(
    sessionPort:
        com.github.prule.sim.tracker.adapter.out.persistence.session.SessionPersistenceAdapter
) {

  com.github.prule.sim.tracker.adapter.`in`.web.session.FindSessionController(
      this,
      com.github.prule.sim.tracker.application.domain.service.session.FindSessionService(
          sessionPort
      ),
  )
  com.github.prule.sim.tracker.adapter.`in`.web.session.StartSessionController(
      this,
      com.github.prule.sim.tracker.application.domain.service.session.StartSessionService(
          sessionPort,
          sessionPort,
      ),
  )
  com.github.prule.sim.tracker.adapter.`in`.web.session.CreateSessionController(
      this,
      com.github.prule.sim.tracker.application.domain.service.session.CreateSessionService(
          sessionPort
      ),
  )
  com.github.prule.sim.tracker.adapter.`in`.web.session.SearchSessionController(
      this,
      com.github.prule.sim.tracker.application.domain.service.session.SearchSessionService(
          sessionPort
      ),
  )
  com.github.prule.sim.tracker.adapter.`in`.web.session.FinishSessionController(
      this,
      com.github.prule.sim.tracker.application.domain.service.session.FinishSessionService(
          sessionPort,
          sessionPort,
      ),
  )
}

private fun Application.initializeLapControllers(
    sessionPort:
        com.github.prule.sim.tracker.adapter.out.persistence.session.SessionPersistenceAdapter
) {
  val mapper = com.github.prule.sim.tracker.adapter.out.persistence.lap.LapMapper()
  val lapPort =
      com.github.prule.sim.tracker.adapter.out.persistence.lap.LapPersistenceAdapter(
          com.github.prule.sim.tracker.adapter.out.persistence.lap.LapRepository(mapper),
          mapper,
      )

  com.github.prule.sim.tracker.adapter.`in`.web.lap.CreateLapController(
      this,
      com.github.prule.sim.tracker.application.domain.service.lap.CreateLapService(
          lapPort,
          sessionPort,
      ),
  )
  com.github.prule.sim.tracker.adapter.`in`.web.lap.SearchLapController(
      this,
      com.github.prule.sim.tracker.application.domain.service.lap.SearchLapService(lapPort),
  )
}
