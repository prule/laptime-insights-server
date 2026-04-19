package com.github.prule.laptimeinsights.adapter.`in`.web.session

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable

@Serializable
@Resource("/api/1/sessions")
data class SessionRoutes(val dummy: String? = null) {
  @Serializable @Resource("/options") data class Options(val parent: SessionRoutes = SessionRoutes())

  @Serializable
  @Resource("/{uid}")
  data class SessionId(val parent: SessionRoutes = SessionRoutes(), val uid: String) {
    @Serializable @Resource("/start") data class Start(val parent: SessionId)

    @Serializable @Resource("/finish") data class Finish(val parent: SessionId)
  }
}
