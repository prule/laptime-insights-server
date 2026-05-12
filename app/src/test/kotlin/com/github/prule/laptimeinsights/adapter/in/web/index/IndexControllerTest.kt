package com.github.prule.laptimeinsights.adapter.`in`.web.index

import com.github.prule.laptimeinsights.AppModule
import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class IndexControllerTest {

  private fun parseLinks(body: String): Map<String, String> {
    val root = Json.parseToJsonElement(body).jsonObject
    val links = root["_links"]?.jsonObject ?: error("missing _links: $body")
    return links.mapValues { it.value.jsonPrimitive.content }
  }

  @Test
  fun `returns all links when every feature is enabled`() = testApplication {
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-index-all;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet(),
      )
    }

    val response = client.get("/api/1")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val links = parseLinks(response.bodyAsText())
    assertThat(links.keys)
      .containsExactlyInAnyOrder(
        "self",
        "overview",
        "sessions",
        "sessionOptions",
        "laps",
        "compare",
        "live",
      )
    assertThat(links["self"]).isEqualTo("/api/1")
    assertThat(links["overview"]).isEqualTo("/api/1/sessions")
    assertThat(links["sessions"]).isEqualTo("/api/1/sessions")
    assertThat(links["sessionOptions"]).isEqualTo("/api/1/sessions/options")
    assertThat(links["laps"]).isEqualTo("/api/1/laps")
    assertThat(links["compare"]).isEqualTo("/api/1/laps/compare")
    assertThat(links["live"]).isEqualTo("/api/1/events")
  }

  @Test
  fun `omits links for disabled features`() = testApplication {
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-index-partial;DB_CLOSE_DELAY=-1;",
        enabledFeatures = setOf(Feature.SESSIONS, Feature.LAPS),
      )
    }

    val links = parseLinks(client.get("/api/1").bodyAsText())

    assertThat(links.keys).containsExactlyInAnyOrder("self", "sessions", "sessionOptions", "laps")
    assertThat(links).doesNotContainKeys("overview", "compare", "live")
  }

  @Test
  fun `returns only self when all features disabled`() = testApplication {
    application {
      module(
        configuration = ApplicationConfiguration(),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-index-none;DB_CLOSE_DELAY=-1;",
        enabledFeatures = emptySet(),
      )
    }

    val links = parseLinks(client.get("/api/1").bodyAsText())

    assertThat(links).containsOnlyKeys("self")
  }
}
