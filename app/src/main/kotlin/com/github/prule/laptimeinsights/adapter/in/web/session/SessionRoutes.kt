package com.github.prule.laptimeinsights.adapter.`in`.web.session

import io.ktor.resources.Resource

@Resource("/api/1/sessions")
class SessionRoutes {
  @Resource("/{uid}")
  class SessionId(val parent: SessionRoutes = SessionRoutes(), val uid: String) {
    @Resource("/start") class Start(val parent: SessionId)

    @Resource("/finish") class Finish(val parent: SessionId)
  }
}
