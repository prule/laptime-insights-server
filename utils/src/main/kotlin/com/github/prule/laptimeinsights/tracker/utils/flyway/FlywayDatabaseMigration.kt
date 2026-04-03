package com.github.prule.acc.client.utils.com.github.prule.sim.tracker.utils.flyway

import javax.sql.DataSource
import org.flywaydb.core.Flyway

class FlywayDatabaseMigration {
  fun migrate(datasource: DataSource) {
    flyway(datasource).migrate()
  }

  fun flyway(datasource: DataSource): Flyway =
      Flyway.configure()
          //            .defaultSchema("simtracker")
          .baselineOnMigrate(true)
          .baselineVersion("0")
          .dataSource(datasource)
          .locations("classpath:db/migrations")
          .load()
}
