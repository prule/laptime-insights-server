package com.github.prule.laptimeinsights.adapter.`in`.web

import com.github.prule.laptimeinsights.AppModule
import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * Verifies the rels every resource is expected to carry — the frontend follows these instead of
 * hard-coding URLs, so a regression here breaks navigation silently.
 */
class ResourceLinksTest {

  private fun linksOf(body: String): Map<String, String> {
    val root = Json.parseToJsonElement(body).jsonObject
    val links = root["_links"]?.jsonObject ?: error("missing _links: $body")
    return links.mapValues { it.value.jsonPrimitive.content }
  }

  private fun firstItemLinks(body: String): Map<String, String> {
    val items = Json.parseToJsonElement(body).jsonObject["items"]?.jsonArray ?: error("no items")
    val first = items.firstOrNull()?.jsonObject ?: error("empty items: $body")
    val links = first["_links"]?.jsonObject ?: error("missing _links: $body")
    return links.mapValues { it.value.jsonPrimitive.content }
  }

  @Test
  fun `SessionResource exposes self and laps rels`() = testApplication {
    val appModule = AppModule()
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = appModule,
        jdbcUrl = "jdbc:h2:mem:test-links-session;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet(),
      )
    }

    startApplication()
    val session = transaction {
      appModule.session.createSessionUseCase.createSession(
        CreateSessionCommand(
          simulator = Simulator.ACC,
          sessionType = SessionType("Practice"),
          track = Track("Monza"),
          car = Car("Ferrari"),
        )
      )
    }

    val response = client.get("/api/1/sessions/${session.uid.value}")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val links = linksOf(response.bodyAsText())
    assertThat(links).containsKeys("self", "laps")
    assertThat(links["self"]).isEqualTo("/api/1/sessions/${session.uid.value}")
    assertThat(links["laps"]).isEqualTo("/api/1/laps?sessionUid=${session.uid.value}")
  }

  @Test
  fun `LapResource exposes self, session and telemetry rels`() = testApplication {
    val appModule = AppModule()
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = appModule,
        jdbcUrl = "jdbc:h2:mem:test-links-lap;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet(),
      )
    }

    startApplication()
    val session = transaction {
      appModule.session.createSessionUseCase.createSession(
        CreateSessionCommand(
          simulator = Simulator.ACC,
          sessionType = SessionType("Practice"),
          track = Track("Monza"),
          car = Car("Ferrari"),
        )
      )
    }
    transaction {
      appModule.lap.createLapUseCase.createLap(
        CreateLapCommand(
          sessionUid = session.uid,
          carId = com.github.prule.laptimeinsights.application.domain.model.CarId(1),
          car = Car("Ferrari"),
          recordedAt = Instant.fromEpochMilliseconds(0),
          lapTime = LapTimeMs(90_000),
          lapNumber = LapNumber(1),
          valid = ValidLap(true),
        )
      )
    }

    val response = client.get("/api/1/laps?sessionUid=${session.uid.value}")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val links = firstItemLinks(response.bodyAsText())
    assertThat(links).containsKeys("self", "session", "telemetry")
    assertThat(links["session"]).isEqualTo("/api/1/sessions/${session.uid.value}")
    val selfPath = links["self"]!!
    val lapUid = selfPath.substringAfterLast('/')
    assertThat(links["telemetry"]).isEqualTo("/api/1/laps/$lapUid/telemetry")
  }

  @Test
  fun `SessionOptionsResource exposes self and sessions rels`() = testApplication {
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-links-options;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet(),
      )
    }

    val response = client.get("/api/1/sessions/options")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val links = linksOf(response.bodyAsText())
    assertThat(links).containsKeys("self", "sessions")
    assertThat(links["sessions"]).isEqualTo("/api/1/sessions")
  }

  @Test
  fun `SessionResource omits laps rel when laps feature is off`() = testApplication {
    val appModule = AppModule()
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = appModule,
        jdbcUrl = "jdbc:h2:mem:test-links-session-no-laps;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet() - Feature.LAPS,
      )
    }

    startApplication()
    val session = transaction {
      appModule.session.createSessionUseCase.createSession(
        CreateSessionCommand(
          simulator = Simulator.ACC,
          sessionType = SessionType("Practice"),
          track = Track("Monza"),
          car = Car("Ferrari"),
        )
      )
    }

    val links = linksOf(client.get("/api/1/sessions/${session.uid.value}").bodyAsText())
    assertThat(links).containsKey("self").doesNotContainKey("laps")
  }

  @Test
  fun `LapResource omits session rel when sessions feature is off`() = testApplication {
    val appModule = AppModule()
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = appModule,
        jdbcUrl = "jdbc:h2:mem:test-links-lap-no-session;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet() - Feature.SESSIONS,
      )
    }

    startApplication()
    val session = transaction {
      appModule.session.createSessionUseCase.createSession(
        CreateSessionCommand(
          simulator = Simulator.ACC,
          sessionType = SessionType("Practice"),
          track = Track("Monza"),
          car = Car("Ferrari"),
        )
      )
    }
    transaction {
      appModule.lap.createLapUseCase.createLap(
        CreateLapCommand(
          sessionUid = session.uid,
          carId = com.github.prule.laptimeinsights.application.domain.model.CarId(1),
          car = Car("Ferrari"),
          recordedAt = Instant.fromEpochMilliseconds(0),
          lapTime = LapTimeMs(90_000),
          lapNumber = LapNumber(1),
          valid = ValidLap(true),
        )
      )
    }

    val links =
      firstItemLinks(client.get("/api/1/laps?sessionUid=${session.uid.value}").bodyAsText())
    assertThat(links).containsKeys("self", "telemetry").doesNotContainKey("session")
  }
}
