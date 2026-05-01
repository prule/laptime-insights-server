plugins {
  // Apply the shared build logic from a convention plugin.
  // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
  id("buildsrc.convention.kotlin-jvm")

  // Apply the Application plugin to add support for building an executable JVM application.
  application
  alias(libs.plugins.flyway)
  alias(libs.plugins.ktfmt)
  alias(libs.plugins.kotlinPluginSerialization)
}

ktfmt {
  // Google style - 2 space indentation & automatically adds/removes trailing commas
  googleStyle()
}

dependencies {
  implementation("com.github.prule:acc-client:main-SNAPSHOT")
  implementation("com.github.prule:acc-messages:main-SNAPSHOT")

  implementation("io.github.xn32:json5k:0.3.0")

  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.server.resources)
  implementation(ktorLibs.server.statusPages)
  implementation(ktorLibs.server.compression)
  implementation(ktorLibs.server.dataConversion)
  implementation(ktorLibs.server.openapi)
  implementation(ktorLibs.server.swagger)
  implementation(ktorLibs.server.websockets)

  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(libs.bundles.exposed)
  implementation(libs.h2)
  implementation(libs.logback.classic)
  implementation("com.zaxxer:HikariCP:3.4.5")

  implementation(project(":utils"))

  testImplementation(libs.junit.jupiter.api)
  testImplementation(libs.junit.jupiter.params)
  testRuntimeOnly(libs.junit.jupiter.engine)
  testImplementation(libs.assertj.core)
  testImplementation(libs.mockk)
  testImplementation(libs.rest.assured)
  testImplementation(libs.rest.assured.kotlin)
  testRuntimeOnly("org.junit.platform:junit-platform-launcher")

  testImplementation(libs.ktor.server.test.host)
  testImplementation(libs.ktor.client.websockets)
}

application { mainClass = "com.github.prule.acc.client.app.AppKt" }
