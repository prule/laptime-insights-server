// The settings file is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins,
// dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

dependencyResolutionManagement {
  versionCatalogs { create("ktorLibs") { from("io.ktor:ktor-version-catalog:3.5.0") } }

  // Use Maven Central as the default repository (where Gradle will download dependencies) in all
  // subprojects.
  @Suppress("UnstableApiUsage")
  repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
  }
}

plugins {
  // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":app")

include(":utils")

include(":frontend")

rootProject.name = "lap-insights-server"
