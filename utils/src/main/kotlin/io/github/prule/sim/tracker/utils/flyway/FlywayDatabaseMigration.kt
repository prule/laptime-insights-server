package io.github.prule.acc.client.utils.io.github.prule.sim.tracker.utils.flyway

import org.flywaydb.core.Flyway
import javax.sql.DataSource

class FlywayDatabaseMigration {
    fun migrate(datasource: DataSource) {
        flyway(datasource).migrate()
    }

    fun flyway(datasource: DataSource): Flyway =
        Flyway
            .configure()
//            .defaultSchema("simtracker")
            .baselineOnMigrate(true)
            .baselineVersion("0")
            .dataSource(datasource)
            .locations("classpath:db/migrations")
            .load()
}
