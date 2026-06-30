package com.github.prule.laptimeinsights.adapter.`in`.web.profile

import io.ktor.resources.Resource

@Resource("/api/1/public-profile")
class ProfileRoutes {
  /** Toggle action: `PUT` sets the public profile on/off. */
  @Resource("/enabled") class Enabled(val parent: ProfileRoutes = ProfileRoutes())
}
