package com.github.prule.laptimeinsights.adapter.`in`.web.index

import com.github.prule.laptimeinsights.Feature
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller exposing **`GET /api/1`** — the HATEOAS entry point. The response separates two
 * concerns the frontend needs at boot:
 *
 * - `_links` lists every API capability (always present). The frontend follows these to fetch data;
 *   this list does not shrink when a UI feature is hidden, so screens that compose data from
 *   multiple features (e.g. Overview reading sessions + laps) keep working.
 * - `enabledFeatures` lists the UI surfaces that should be visible. The sidebar, router, and
 *   cross-screen buttons gate on this.
 */
@OptIn(ExperimentalKtorApi::class)
class IndexController(application: Application, enabledFeatures: Set<Feature>) {
  init {
    val linkFactory = IndexLinkFactory(application)
    val features = enabledFeatures.map { it.rel }.sorted()
    application.routing {
      get<IndexRoutes> {
          call.respond(IndexResource(_links = linkFactory.build(Unit), enabledFeatures = features))
        }
        .describe {
          summary = "API index"
          description =
            """
            HATEOAS bootstrap endpoint.

            `_links` advertises the full set of API capabilities — every rel is always present so
            screens that compose data from multiple features keep working when a UI surface is
            hidden. `enabledFeatures` lists the UI surfaces the operator has turned on via the
            `FEATURE_<NAME>` environment variables (or the in-code defaults); the frontend uses
            this list to decide which nav items, routes, and action buttons to render.

            Stable link relations: `self`, `overview`, `sessions`, `sessionOptions`,
            `sessionsAggregate`, `laps`, `lapsAggregate`, `compare`, `live`.

            Feature ids: `overview`, `sessions`, `laps`, `compare`, `live`.
            """
              .trimIndent()

          responses { HttpStatusCode.OK { description = "The API index resource." } }
        }
    }
  }
}
