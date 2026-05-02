package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.adapter.`in`.web.lap.FindLapController
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.SearchLapController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.FindSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SearchOptionsController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SearchSessionController
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SessionEventController
import com.github.prule.laptimeinsights.adapter.out.persistence.JsonFileConfigurationRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.OpenApiInfo
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException as DomainNotFoundException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.openapi.openAPI
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.resources.Resources
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.OpenApiDocSource
import io.ktor.server.routing.routing
import io.ktor.server.routing.routingRoot
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main(args: Array<String>): Unit = runBlocking {
  val configuration = JsonFileConfigurationRepository().loadConfiguration(args[0])
  embeddedServer(factory = Netty, port = configuration.port) { module(configuration) }
    .start(wait = true)
}

fun Application.module(
  configuration: ApplicationConfiguration,
  appModule: AppModule = AppModule(),
  jdbcUrl: String = EnvironmentVariables.jdbcUrl(),
) {
  install(Resources)
  install(StatusPages) {
    exception<IllegalStateException> { call, cause ->
      call.respond(HttpStatusCode.BadRequest, mapOf("error" to cause.message))
    }
    // Ktor itself raises this for things like missing Resources routes; keep it mapped.
    exception<NotFoundException> { call, _ -> call.respond(HttpStatusCode.NotFound) }
    // The application layer raises its own `NotFoundException` so it doesn't depend on Ktor.
    exception<DomainNotFoundException> { call, _ -> call.respond(HttpStatusCode.NotFound) }
  }

  install(ContentNegotiation) { json(Json { encodeDefaults = true }) }

  install(WebSockets) {
    pingPeriod = 15.seconds
    timeout = 15.seconds
    maxFrameSize = Long.MAX_VALUE
    masking = false
    contentConverter = io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter(Json)
  }

  DatabaseFactory.init(jdbcUrl)

  initializeSessionControllers(appModule)
  initializeLapControllers(appModule)
  SessionEventController(this, appModule.eventPort)

  routing {
    swaggerUI("/swaggerUI") {
      info =
        OpenApiInfo(
          title = "Laptime Insights API",
          version = "1.0",
          description = "API for tracking and analyzing racing simulator lap times.",
        )
      source = OpenApiDocSource.Routing(ContentType.Application.Json) { routingRoot.descendants() }
    }

    openAPI(path = "openapi") {
      info =
        OpenApiInfo(
          title = "Laptime Insights API",
          version = "1.0",
          description = "API for tracking and analyzing racing simulator lap times.",
        )
      source = OpenApiDocSource.Routing { routingRoot.descendants() }
    }
  }

  launch(Dispatchers.IO) {
    ClientInitializer(appModule).initializeClient(configuration.clientConfiguration)
  }
}

private fun Application.initializeSessionControllers(appModule: AppModule) {
  FindSessionController(this, appModule.session.findSessionUseCase)
  SearchSessionController(this, appModule.session.searchSessionUseCase)
  SearchOptionsController(this, appModule.session.searchSessionOptionsUseCase)
}

private fun Application.initializeLapControllers(appModule: AppModule) {
  SearchLapController(this, appModule.lap.searchLapUseCase)
  FindLapController(this, appModule.lap.findLapUseCase)
}
