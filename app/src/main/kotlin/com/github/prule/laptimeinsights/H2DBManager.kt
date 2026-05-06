package com.github.prule.laptimeinsights

import org.h2.tools.Server
import org.slf4j.LoggerFactory

/**
 * Allows the starting and stopping of H2 web/tcp servers. Useful during development to look into
 * the database using development tools.
 */
class H2DBManager(val web: Boolean, val tcp: Boolean) {
  private val logger = LoggerFactory.getLogger(javaClass)
  private var webServer: Server? = null
  private var tcpServer: Server? = null

  fun start() {
    if (web) {
      logger.warn("Starting database Web Server")
      webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    }
    if (tcp) {
      logger.warn("Starting database tcp Server")
      tcpServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start()
    }
  }

  fun stop() {
    webServer?.stop()
    tcpServer?.stop()
  }
}
