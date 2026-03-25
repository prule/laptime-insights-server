package io.github.prule.sim.tracker.adapter.`in`.web

import io.ktor.resources.*

@Resource("/api/1/sessions")
class SessionRoutes {
  @Resource("/{uid}")
  class SessionId(val parent: SessionRoutes = SessionRoutes(), val uid: String) {
    @Resource("/start") class Start(val parent: SessionId)
  }
}
