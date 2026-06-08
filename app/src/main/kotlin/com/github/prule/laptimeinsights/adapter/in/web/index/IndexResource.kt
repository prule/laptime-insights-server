package com.github.prule.laptimeinsights.adapter.`in`.web.index

import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapRoutes
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SessionRoutes
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable

/**
 * Entry-point resource returned by `GET /api/1`.
 *
 * Two orthogonal concerns travel here:
 * - `_links` advertises **API capabilities** (the data plane). These rels are always emitted so
 *   that an enabled UI feature can read sessions/laps data even when the sessions/laps *UI* is
 *   hidden. Without that, disabling Sessions would break Overview, which consumes the sessions
 *   feed.
 * - `enabledFeatures` advertises **UI surfaces** that are turned on. The frontend's
 *   `useFeatureEnabled(feature)` reads from this list to decide whether to render nav items,
 *   routes, and cross-screen action buttons.
 */
@Serializable
data class IndexResource(
  val _links: Map<String, String>,
  /** Stable feature ids that are currently enabled (mirrors [Feature.rel]). */
  val enabledFeatures: List<String>,
)

class IndexLinkFactory(private val application: Application) : LinkFactory<Unit> {
  override fun build(resource: Unit): Map<String, String> =
    linkedMapOf(
      "self" to application.href(IndexRoutes()),
      // Overview shares the sessions feed; expose it under its own rel so a frontend that wants
      // an "all sessions" data entry point can find it via either name.
      Feature.OVERVIEW.rel to application.href(SessionRoutes()),
      Feature.SESSIONS.rel to application.href(SessionRoutes()),
      "sessionOptions" to application.href(SessionRoutes.Options()),
      "sessionsAggregate" to application.href(SessionRoutes.Aggregate()),
      Feature.LAPS.rel to application.href(LapRoutes()),
      "lapsAggregate" to application.href(LapRoutes.Aggregate()),
      Feature.COMPARE.rel to application.href(LapRoutes.Compare()),
      Feature.LIVE.rel to "/api/1/events",
    )
}
