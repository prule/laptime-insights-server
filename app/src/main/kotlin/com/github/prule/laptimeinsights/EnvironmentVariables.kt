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
   * Feature toggle. The in-code [Feature.defaultEnabled] applies unless the matching env var is
   * set: truthy values (`true`, `1`, `yes`) force the feature on, falsy values (`false`, `0`, `no`)
   * force it off, anything else falls back to the default. The override-via-env-var pattern lets
   * ops flip a feature without redeploying while keeping the source of truth in code.
   */
  fun featureEnabled(feature: Feature): Boolean {
    val value = System.getenv(feature.envVar) ?: return feature.defaultEnabled
    return when (value.lowercase()) {
      "true",
      "1",
      "yes" -> true
      "false",
      "0",
      "no" -> false
      else -> feature.defaultEnabled
    }
  }

  fun enabledFeatures(): Set<Feature> = Feature.entries.filter { featureEnabled(it) }.toSet()

  private fun parseBoolean(envVar: String): Boolean {
    val value = System.getenv(envVar) ?: return false
    return value.lowercase() in setOf("true", "1", "yes")
  }
}
