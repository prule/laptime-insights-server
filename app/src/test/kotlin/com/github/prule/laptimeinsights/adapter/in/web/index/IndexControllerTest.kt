package com.github.prule.laptimeinsights.adapter.`in`.web.index

import com.github.prule.laptimeinsights.AppModule
import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.PublicProfileConfig
import com.github.prule.laptimeinsights.module
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
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

  private fun parseEnabledFeatures(body: String): List<String> {
    val root = Json.parseToJsonElement(body).jsonObject
    val arr = root["enabledFeatures"]?.jsonArray ?: error("missing enabledFeatures: $body")
    return arr.map { it.jsonPrimitive.content }
  }

  private val ALL_LINKS =
    listOf(
      "self",
      "overview",
      "sessions",
      "sessionOptions",
      "sessionsAggregate",
      "laps",
      "lapsAggregate",
      "compare",
      "live",
      // Always advertised so the UI can switch the public profile on, even while it's off.
      "publicProfileToggle",
    )

  @Test
  fun `_links always advertises every capability regardless of toggle state`() = testApplication {
    application {
      module(
        // Pin the (now on-by-default) public profile off so these env-driven feature assertions
        // stay focused; public-profile gating has its own test.
        configuration =
          ApplicationConfiguration(publicProfile = PublicProfileConfig(enabled = false)),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-index-all;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet(),
      )
    }

    val response = client.get("/api/1")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    val links = parseLinks(response.bodyAsText())
    assertThat(links.keys).containsExactlyInAnyOrderElementsOf(ALL_LINKS)
    assertThat(links["self"]).isEqualTo("/api/1")
    assertThat(links["overview"]).isEqualTo("/api/1/sessions")
    assertThat(links["sessions"]).isEqualTo("/api/1/sessions")
    assertThat(links["sessionOptions"]).isEqualTo("/api/1/sessions/options")
    assertThat(links["sessionsAggregate"]).isEqualTo("/api/1/sessions/aggregate")
    assertThat(links["laps"]).isEqualTo("/api/1/laps")
    assertThat(links["lapsAggregate"]).isEqualTo("/api/1/laps/aggregate")
    assertThat(links["compare"]).isEqualTo("/api/1/laps/compare")
    assertThat(links["live"]).isEqualTo("/api/1/events")
  }

  @Test
  fun `enabledFeatures lists every Feature when all are on`() = testApplication {
    application {
      module(
        // Pin the (now on-by-default) public profile off so these env-driven feature assertions
        // stay focused; public-profile gating has its own test.
        configuration =
          ApplicationConfiguration(publicProfile = PublicProfileConfig(enabled = false)),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-index-features-all;DB_CLOSE_DELAY=-1;",
        enabledFeatures = Feature.entries.toSet(),
      )
    }

    val features = parseEnabledFeatures(client.get("/api/1").bodyAsText())
    assertThat(features)
      .containsExactlyInAnyOrder("overview", "sessions", "laps", "compare", "live")
  }

  @Test
  fun `enabledFeatures shrinks but _links stays complete when a feature is off`() =
    testApplication {
      application {
        module(
          // Pin the (now on-by-default) public profile off so these env-driven feature assertions
          // stay focused; public-profile gating has its own test.
          configuration =
            ApplicationConfiguration(publicProfile = PublicProfileConfig(enabled = false)),
          appModule = AppModule(),
          jdbcUrl = "jdbc:h2:mem:test-index-partial;DB_CLOSE_DELAY=-1;",
          enabledFeatures = setOf(Feature.OVERVIEW, Feature.LAPS),
        )
      }

      val body = client.get("/api/1").bodyAsText()
      // _links is unchanged: the data plane stays usable when only some UI surfaces are enabled.
      assertThat(parseLinks(body).keys).containsExactlyInAnyOrderElementsOf(ALL_LINKS)
      // enabledFeatures reflects the toggle.
      assertThat(parseEnabledFeatures(body)).containsExactlyInAnyOrder("overview", "laps")
    }

  @Test
  fun `enabledFeatures is empty when every UI feature is off`() = testApplication {
    application {
      module(
        // Pin the (now on-by-default) public profile off so these env-driven feature assertions
        // stay focused; public-profile gating has its own test.
        configuration =
          ApplicationConfiguration(publicProfile = PublicProfileConfig(enabled = false)),
        appModule = AppModule(),
        jdbcUrl = "jdbc:h2:mem:test-index-none;DB_CLOSE_DELAY=-1;",
        enabledFeatures = emptySet(),
      )
    }

    val body = client.get("/api/1").bodyAsText()
    assertThat(parseLinks(body).keys).containsExactlyInAnyOrderElementsOf(ALL_LINKS)
    assertThat(parseEnabledFeatures(body)).isEmpty()
  }
}
