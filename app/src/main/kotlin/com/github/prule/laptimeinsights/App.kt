package com.github.prule.laptimeinsights

import com.github.prule.acc.client.AccClient
import com.github.prule.acc.client.AccClientConfiguration
import com.github.prule.acc.client.ClientState
import com.github.prule.acc.client.LoggingListener
import com.github.prule.acc.client.RegistrationResultListener
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.CreateLapController
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.SearchLapController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.CreateSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.FindSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.FinishSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SearchSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.StartSessionController
import com.github.prule.laptimeinsights.adapter.out.persistence.JsonFileConfigurationRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionRepository
import com.github.prule.laptimeinsights.application.domain.service.lap.CreateLapService
import com.github.prule.laptimeinsights.application.domain.service.lap.SearchLapService
import com.github.prule.laptimeinsights.application.domain.service.session.CreateSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.FindSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.FinishSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.SearchSessionService
import com.github.prule.laptimeinsights.application.domain.service.session.StartSessionService
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

fun main(args: Array<String>): Unit = runBlocking {
  val configuration = JsonFileConfigurationRepository().loadConfiguration(args[0])
  embeddedServer(
          factory = Netty,
          port = configuration.port,
      ) {
        module(configuration)
      }
      .start(wait = true)
}

fun Application.module(configuration: ApplicationConfiguration) {
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
  val sessionPort =
      SessionPersistenceAdapter(
          SessionRepository(mapper),
          mapper,
      )

  initializeSessionControllers(sessionPort)
  initializeLapControllers(sessionPort)

  initializeClient(configuration.clientConfiguration)

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

private fun initializeClient(configuration: ApplicationClientConfiguration) = runBlocking {
  val clientState = ClientState()
  AccClient(
          AccClientConfiguration(
              "Test",
              port = configuration.port,
              serverIp = configuration.serverIp,
          ),
      )
      .connect(
          listOf(
              LoggingListener(),
              RegistrationResultListener(clientState),
          ),
      )
}

private fun Application.initializeSessionControllers(sessionPort: SessionPersistenceAdapter) {

  FindSessionController(
      this,
      FindSessionService(sessionPort),
  )
  StartSessionController(
      this,
      StartSessionService(
          sessionPort,
          sessionPort,
      ),
  )
  CreateSessionController(
      this,
      CreateSessionService(sessionPort),
  )
  SearchSessionController(
      this,
      SearchSessionService(sessionPort),
  )
  FinishSessionController(
      this,
      FinishSessionService(
          sessionPort,
          sessionPort,
      ),
  )
}

private fun Application.initializeLapControllers(sessionPort: SessionPersistenceAdapter) {
  val mapper = LapMapper()

  val lapPort =
      LapPersistenceAdapter(
          LapRepository(mapper),
          mapper,
      )

  CreateLapController(
      this,
      CreateLapService(
          lapPort,
          sessionPort,
      ),
  )
  SearchLapController(
      this,
      SearchLapService(lapPort),
  )
}
