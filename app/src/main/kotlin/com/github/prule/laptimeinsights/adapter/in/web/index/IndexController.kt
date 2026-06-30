package com.github.prule.laptimeinsights.adapter.`in`.web.index

import com.github.prule.laptimeinsights.ConfigurationStore
import com.github.prule.laptimeinsights.Feature
import com.github.prule.laptimeinsights.adapter.`in`.web.profile.ProfileRoutes
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.resources.get
import io.ktor.server.resources.href
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
 *
 * Most features are resolved once at startup from env vars ([baseEnabledFeatures]).
 * `public-profile` is the exception: its enabled state is read **per request** from [configStore]
 * so the runtime toggle takes effect without a restart. When enabled, its capability `_link` and
 * `enabledFeatures` flag appear; the toggle action link is always advertised so the UI can turn it
 * on.
 */
@OptIn(ExperimentalKtorApi::class)
class IndexController(
  application: Application,
  baseEnabledFeatures: Set<Feature>,
  configStore: ConfigurationStore,
) {
  init {
    val linkFactory = IndexLinkFactory(application)
    // Env-resolved features minus public-profile, which is config-driven (resolved per request).
    val staticFeatures = baseEnabledFeatures - Feature.PUBLIC_PROFILE
    application.routing {
      get<IndexRoutes> {
          val profileEnabled = configStore.configuration.publicProfile.enabled
          val features =
            buildList {
                addAll(staticFeatures.map { it.rel })
                if (profileEnabled) add(Feature.PUBLIC_PROFILE.rel)
              }
              .sorted()

          val links = linkFactory.build(Unit).toMutableMap()
          // Toggle action is always available so the UI can switch the profile on.
          links["publicProfileToggle"] = application.href(ProfileRoutes.Enabled())
          // The data capability link is present only when the profile is enabled.
          if (profileEnabled) {
            links[Feature.PUBLIC_PROFILE.rel] = application.href(ProfileRoutes())
          }

          call.respond(IndexResource(_links = links, enabledFeatures = features))
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

            `public-profile` is resolved at request time from the persisted toggle: its `_link` and
            `enabledFeatures` flag appear only when enabled. The `publicProfileToggle` action link
            is always present.

            Stable link relations: `self`, `overview`, `sessions`, `sessionOptions`,
            `sessionsAggregate`, `laps`, `lapsAggregate`, `compare`, `live`, `publicProfileToggle`.

            Feature ids: `overview`, `sessions`, `laps`, `compare`, `live`, `public-profile`.
            """
              .trimIndent()

          responses { HttpStatusCode.OK { description = "The API index resource." } }
        }
    }
  }
}
