package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.adapter.`in`.web.lap.CreateLapController
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.SearchLapController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.CreateSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.FindSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.FinishSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SearchOptionsController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SearchSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SessionEventController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.StartSessionController
import com.github.prule.laptimeinsights.adapter.out.persistence.JsonFileConfigurationRepository
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
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

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

  install(WebSockets) {
    pingPeriod = 15.seconds
    timeout = 15.seconds
    maxFrameSize = Long.MAX_VALUE
    masking = false
    contentConverter = io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter(Json)
  }

  DatabaseFactory.init()

  val appModule = AppModule()
  initializeSessionControllers(appModule)
  initializeLapControllers(appModule)
  SessionEventController(this, appModule.eventPort)

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

  launch(Dispatchers.IO) { ClientInitializer(appModule).initializeClient(configuration.clientConfiguration) }
}

private fun Application.initializeSessionControllers(appModule: AppModule) {
  FindSessionController(
      this,
      appModule.session.findSessionUseCase,
  )
  StartSessionController(
      this,
      appModule.session.startSessionUseCase,
  )
  CreateSessionController(
      this,
      appModule.session.createSessionUseCase,
  )
  SearchSessionController(
      this,
      appModule.session.searchSessionUseCase,
  )
  SearchOptionsController(
      this,
      appModule.session.searchSessionOptionsUseCase,
  )
  FinishSessionController(
      this,
      appModule.session.finishSessionUseCase,
  )
}

private fun Application.initializeLapControllers(appModule: AppModule) {
  CreateLapController(
      this,
      appModule.lap.createLapUseCase,
  )
  SearchLapController(
      this,
      appModule.lap.searchLapUseCase,
  )
}
