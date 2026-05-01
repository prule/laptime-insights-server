package com.github.prule.laptimeinsights.adapter.`in`.web.session

import com.github.prule.laptimeinsights.AppModule
import com.github.prule.laptimeinsights.ApplicationConfiguration
import com.github.prule.laptimeinsights.module
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import java.net.ServerSocket
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionRestAssuredTest {

  private lateinit var server:
    EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
  private var port: Int = 0

  @BeforeAll
  fun setup() {
    port = findFreePort()
    RestAssured.port = port

    server =
      embeddedServer(Netty, port = port) {
          module(
            configuration = ApplicationConfiguration(),
            appModule = AppModule(),
            jdbcUrl =
              "jdbc:h2:mem:test_rest_assured_${System.currentTimeMillis()};DB_CLOSE_DELAY=-1;",
          )
        }
        .start(wait = false)
  }

  @AfterAll
  fun tearDown() {
    server.stop(1000, 1000)
  }

  private fun findFreePort(): Int {
    return ServerSocket(0).use { it.localPort }
  }

  @Test
  fun `should create a new session`() {
    val requestBody =
      """
      {
          "simulator": "ACC",
          "track": "Monza",
          "car": "Ferrari 488 GT3",
          "sessionType": "Race"
      }
      """
        .trimIndent()

    given()
      .contentType(ContentType.JSON)
      .body(requestBody)
      .`when`()
      .post("/api/1/sessions")
      .then()
      .statusCode(201)
      .body("uid", notNullValue())
      .body("track", equalTo("Monza"))
      .body("simulator", equalTo("ACC"))
  }

  @Test
  fun `should return error when starting session twice`() {
    val requestBody =
      """
      {
          "simulator": "ACC",
          "track": "Monza",
          "car": "Ferrari 488 GT3",
          "sessionType": "Race"
      }
      """
        .trimIndent()

    val uid =
      given()
        .contentType(ContentType.JSON)
        .body(requestBody)
        .`when`()
        .post("/api/1/sessions")
        .then()
        .statusCode(201)
        .extract()
        .path<String>("uid")

    val startBody =
      """
      {
          "startedAt": "2026-05-01T10:00:00Z"
      }
      """
        .trimIndent()

    // First start should succeed
    given()
      .contentType(ContentType.JSON)
      .body(startBody)
      .`when`()
      .post("/api/1/sessions/$uid/start")
      .then()
      .statusCode(200)

    // Second start should fail
    given()
      .contentType(ContentType.JSON)
      .body(startBody)
      .`when`()
      .post("/api/1/sessions/$uid/start")
      .then()
      .statusCode(500) // Currently no StatusPages mapping, so 500 is expected
  }
}
