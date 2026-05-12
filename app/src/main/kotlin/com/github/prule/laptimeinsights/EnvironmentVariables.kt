package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.tracker.utils.NotFoundException

object EnvironmentVariables {
  private fun getVar(name: String): String {
    return System.getenv(name) ?: throw NotFoundException("Missing environment variable: $name")
  }

  fun jdbcUrl(): String {
    return getVar("JDBC_URL")
  }

  /**
   * When set to a truthy value (`true`, `1`, `yes`), the application populates the database with
   * sample sessions and laps on startup. Intended for local development and demos only.
   */
  fun shouldSeedDatabase() = parseBoolean("DB_SEED")

  /** When truthy starts an H2 web console. */
  fun h2web() = parseBoolean("H2_WEB")

  /** When truthy starts an H2 tcp server. */
  fun h2tcp() = parseBoolean("H2_TCP")

  /**
   * Feature toggle. Each [Feature] is enabled by default; set the corresponding env var to a falsy
   * value (`false`, `0`, `no`) to hide the feature's link from `GET /api/1`.
   */
  fun featureEnabled(feature: Feature): Boolean {
    val value = System.getenv(feature.envVar) ?: return true
    return value.lowercase() !in setOf("false", "0", "no")
  }

  fun enabledFeatures(): Set<Feature> = Feature.entries.filter { featureEnabled(it) }.toSet()

  private fun parseBoolean(envVar: String): Boolean {
    val value = System.getenv(envVar) ?: return false
    return value.lowercase() in setOf("true", "1", "yes")
  }
}
