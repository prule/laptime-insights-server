package com.github.prule.laptimeinsights

import org.h2.tools.Server

/**
 * Allows the starting and stopping of H2 web/tcp servers. Useful during development to look into
 * the database using development tools.
 */
class H2DBManager(val web: Boolean, val tcp: Boolean) {

  private var webServer: Server? = null
  private var tcpServer: Server? = null

  fun start() {
    if (web) {
      webServer = Server.createWebServer("-web", "-webAllowOthers", "-webPort", "8082").start()
    }
    if (tcp) {
      tcpServer = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9092").start()
    }
  }

  fun stop() {
    webServer?.stop()
    tcpServer?.stop()
  }
}
