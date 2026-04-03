plugins {
  // Apply the shared build logic from a convention plugin.
  // The shared code is located in `buildSrc/src/main/kotlin/kotlin-jvm.gradle.kts`.
  id("buildsrc.convention.kotlin-jvm")

  // Apply the Application plugin to add support for building an executable JVM application.
  application
  alias(libs.plugins.flyway)
  alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
  implementation("com.github.prule:acc-client:main-SNAPSHOT")
  implementation("com.github.prule:acc-messages:main-SNAPSHOT")

  implementation(ktorLibs.server.core)
  implementation(ktorLibs.server.contentNegotiation)
  implementation(ktorLibs.server.netty)
  implementation(ktorLibs.server.resources)
  implementation(ktorLibs.server.statusPages)
  implementation(ktorLibs.server.compression)
  implementation(ktorLibs.server.dataConversion)
  implementation(ktorLibs.server.openapi)
  implementation(ktorLibs.server.swagger)

  implementation(ktorLibs.serialization.kotlinx.json)
  implementation(libs.bundles.exposed)
  implementation(libs.h2)
  implementation(libs.logback.classic)
  implementation("com.zaxxer:HikariCP:3.4.2")

  // Project "app" depends on project "utils". (Project paths are separated with ":", so ":utils"
  // refers to the top-level "utils" project.)
  implementation(project(":utils"))
}

application {
  // Define the Fully Qualified Name for the application main class
  // (Note that Kotlin compiles `App.kt` to a class with FQN `com.example.app.AppKt`.)
  mainClass = "com.github.prule.acc.client.app.AppKt"
}
