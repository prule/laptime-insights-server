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
  fun shouldSeedDatabase(): Boolean {
    val value = System.getenv("DB_SEED") ?: return false
    return value.lowercase() in setOf("true", "1", "yes")
  }
}
