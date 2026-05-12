package com.github.prule.laptimeinsights.adapter.`in`.web.lap

import io.ktor.resources.Resource

@Resource("/api/1/laps")
class LapRoutes {
  @Resource("/compare") class Compare(val parent: LapRoutes = LapRoutes())

  @Resource("/aggregate") class Aggregate(val parent: LapRoutes = LapRoutes())

  @Resource("/{uid}")
  class LapId(val parent: LapRoutes = LapRoutes(), val uid: String) {
    @Resource("/telemetry") class Telemetry(val parent: LapId)
  }
}
