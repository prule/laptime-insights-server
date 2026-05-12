package com.github.prule.laptimeinsights.adapter.`in`.web.index

import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.adapter.`in`.web.LinkFactory
import com.github.prule.laptimeinsights.adapter.`in`.web.lap.LapRoutes
import com.github.prule.laptimeinsights.adapter.`in`.web.session.SessionRoutes
import io.ktor.server.application.Application
import io.ktor.server.resources.href
import kotlinx.serialization.Serializable

/**
 * Entry-point resource returned by `GET /api/1`. The frontend reads `_links` to discover which
 * features the backend currently exposes — a feature whose env-var toggle is off (see [Feature])
 * simply has its link omitted.
 */
@Serializable data class IndexResource(val _links: Map<String, String>)

class IndexLinkFactory(
  private val application: Application,
  private val enabledFeatures: Set<Feature>,
) : LinkFactory<Unit> {
  override fun build(resource: Unit): Map<String, String> {
    val links = linkedMapOf<String, String>()
    links["self"] = application.href(IndexRoutes())
    // Overview is a frontend-only view backed by the sessions feed; it gets its own rel so the
    // frontend can hide just the Overview nav without disabling the sessions API.
    if (Feature.OVERVIEW in enabledFeatures) {
      links[Feature.OVERVIEW.rel] = application.href(SessionRoutes())
    }
    if (Feature.SESSIONS in enabledFeatures) {
      links[Feature.SESSIONS.rel] = application.href(SessionRoutes())
      links["sessionOptions"] = application.href(SessionRoutes.Options())
      links["sessionsAggregate"] = application.href(SessionRoutes.Aggregate())
    }
    if (Feature.LAPS in enabledFeatures) {
      links[Feature.LAPS.rel] = application.href(LapRoutes())
      links["lapsAggregate"] = application.href(LapRoutes.Aggregate())
    }
    if (Feature.COMPARE in enabledFeatures) {
      links[Feature.COMPARE.rel] = application.href(LapRoutes.Compare())
    }
    if (Feature.LIVE in enabledFeatures) {
      links[Feature.LIVE.rel] = "/api/1/events"
    }
    return links
  }
}
