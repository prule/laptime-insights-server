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
 * REST controller exposing **`GET /api/1`** — the HATEOAS entry point. The frontend bootstraps from
 * this single endpoint and uses the returned `_links` to discover which features are turned on for
 * the current backend. Features toggled off via env vars (see [Feature]) are omitted from the link
 * map, so the frontend can hide the corresponding nav items without any hard-coded URLs.
 */
@OptIn(ExperimentalKtorApi::class)
class IndexController(application: Application, enabledFeatures: Set<Feature>) {
  init {
    val linkFactory = IndexLinkFactory(application, enabledFeatures)
    application.routing {
      get<IndexRoutes> { call.respond(IndexResource(_links = linkFactory.build(Unit))) }
        .describe {
          summary = "API index"
          description =
            """
            HATEOAS bootstrap endpoint. The response carries an `_links` map naming every feature
            currently enabled on the backend. A feature that has been disabled via its
            `FEATURE_<NAME>` environment variable is omitted from the map; clients should treat a
            missing link as "feature off" and hide the corresponding UI.

            Stable link relations: `self`, `sessions`, `sessionOptions`, `laps`, `lapsAggregate`,
            `compare`, `live`.
            """
              .trimIndent()

          responses { HttpStatusCode.OK { description = "The API index resource." } }
        }
    }
  }
}
