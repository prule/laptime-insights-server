package com.github.prule.laptimeinsights.adapter.`in`.web

import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.ConfigurationStore
import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.PublicProfileConfig
import com.github.prule.laptimeinsights.module
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PublicProfileControllerTest {

  private fun links(body: String): Map<String, String> =
    Json.parseToJsonElement(body).jsonObject["_links"]!!.jsonObject.mapValues {
      it.value.jsonPrimitive.content
    }

  private fun enabledFeatures(body: String): List<String> =
    Json.parseToJsonElement(body).jsonObject["enabledFeatures"]!!.jsonArray.map {
      it.jsonPrimitive.content
    }

  private fun config(enabled: Boolean) =
    ApplicationConfiguration(publicProfile = PublicProfileConfig(enabled = enabled, name = "Ada"))

  @Test
  fun `profile is gated off by default`() {
    val configStore = ConfigurationStore(config(enabled = false))
    testApplicationWith(configStore) {
      val index = client.get("/api/1").bodyAsText()
      assertThat(links(index)).doesNotContainKey("public-profile")
      // Toggle action is always advertised so the UI can switch it on.
      assertThat(links(index)).containsKey("publicProfileToggle")
      assertThat(enabledFeatures(index)).doesNotContain("public-profile")

      assertThat(client.get("/api/1/public-profile").status).isEqualTo(HttpStatusCode.NotFound)
    }
  }

  @Test
  fun `profile is exposed when enabled`() {
    val configStore = ConfigurationStore(config(enabled = true))
    testApplicationWith(configStore) {
      val index = client.get("/api/1").bodyAsText()
      assertThat(links(index)).containsKey("public-profile")
      assertThat(enabledFeatures(index)).contains("public-profile")
      assertThat(client.get("/api/1/public-profile").status).isEqualTo(HttpStatusCode.OK)
    }
  }

  @Test
  fun `toggle persists in the store and is reflected on the next request`() {
    val configStore = ConfigurationStore(config(enabled = false))
    testApplicationWith(configStore) {
      val response =
        client.put("/api/1/public-profile/enabled") {
          contentType(ContentType.Application.Json)
          setBody("""{"enabled":true}""")
        }
      assertThat(response.status).isEqualTo(HttpStatusCode.OK)
      assertThat(configStore.configuration.publicProfile.enabled).isTrue()

      val index = client.get("/api/1").bodyAsText()
      assertThat(enabledFeatures(index)).contains("public-profile")
      assertThat(links(index)).containsKey("public-profile")
    }
  }

  private fun testApplicationWith(
    configStore: ConfigurationStore,
    block: suspend io.ktor.server.testing.ApplicationTestBuilder.() -> Unit,
  ) =
    io.ktor.server.testing.testApplication {
      application {
        module(
          configuration = configStore.configuration,
          jdbcUrl = "jdbc:h2:mem:test-profile-${System.nanoTime()};DB_CLOSE_DELAY=-1;",
          enabledFeatures = Feature.entries.toSet() - Feature.PUBLIC_PROFILE,
          configStore = configStore,
        )
      }
      startApplication()
      block()
    }
}
