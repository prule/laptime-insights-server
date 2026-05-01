package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.AppModule
import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.module
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.server.testing.testApplication
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeout
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SessionEventControllerTest {

  @Test
  fun `should receive session created event over websocket`() = testApplication {
    val appModule = AppModule()
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = appModule,
        jdbcUrl = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
      )
    }

    val client = createClient { install(WebSockets) }

    client.webSocket("/api/1/events") {
      val session =
        Session(
          id = SessionId(1L),
          uid = Uid(),
          startedAt = null,
          finishedAt = null,
          simulator = Simulator.ACC,
          track = Track("Monza"),
          car = Car("Ferrari"),
          sessionType = SessionType("Race"),
        )

      // Emit the event via the SAME appModule being used by the application
      appModule.eventPort.emit(SessionCreated(session))

      withTimeout(5.seconds) {
        val frame = incoming.receive() as Frame.Text
        val text = frame.readText()

        assertThat(text).contains("\"type\":\"SessionCreated\"")
        assertThat(text).contains("\"data\":")
        assertThat(text).contains(session.uid.value)
        assertThat(text).contains("Monza")
      }
    }
  }
}
