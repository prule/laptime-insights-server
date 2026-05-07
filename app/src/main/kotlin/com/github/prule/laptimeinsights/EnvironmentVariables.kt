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

  private fun parseBoolean(envVar: String): Boolean {
    val value = System.getenv(envVar) ?: return false
    return value.lowercase() in setOf("true", "1", "yes")
  }
}
