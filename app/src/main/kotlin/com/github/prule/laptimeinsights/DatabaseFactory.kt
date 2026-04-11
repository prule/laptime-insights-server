package com.github.prule.laptimeinsights

import com.github.prule.acc.client.utils.com.github.prule.sim.tracker.utils.flyway.FlywayDatabaseMigration
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseFactory {
  fun init() {
    val datasource = hikari()
    Database.connect(datasource)
    FlywayDatabaseMigration().migrate(datasource)
  }

  private fun hikari(): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = "org.h2.Driver"
    config.jdbcUrl = EnvironmentVariables.jdbcUrl()
    config.maximumPoolSize = 3
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
  }
}
