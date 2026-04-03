package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import io.ktor.resources.Resource

@Resource("/api/1/laps")
class LapRoutes {
  @Resource("/{uid}") class LapId(val parent: LapRoutes = LapRoutes(), val uid: String) {}
}
