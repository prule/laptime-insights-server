package com.github.prule.laptimeinsights

import com.github.prule.laptimeinsights.tracker.utils.NotFoundException

object EnvironmentVariables {
  private fun getVar(name: String): String {
    return System.getenv(name) ?: throw NotFoundException("Missing environment variable: $name")
  }

  fun jdbcUrl(): String {
    return getVar("JDBC_URL")
  }
}
