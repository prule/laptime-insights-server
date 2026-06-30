package com.github.prule.laptimeinsights.adapter.`in`.web.profile

import com.github.prule.laptimeinsights.ConfigurationStore
import com.github.prule.laptimeinsights.application.port.`in`.profile.BuildProfileSnapshotUseCase
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.resources.get
import io.ktor.server.resources.put
import io.ktor.server.response.respond
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi

/**
 * REST controller for the public profile.
 *
 * - `GET /api/1/public-profile` → the generated snapshot JSON. Returns `404` when the profile is
 *   disabled, consistent with the HATEOAS convention (the index also omits the link when off).
 * - `PUT /api/1/public-profile/enabled` → flips the runtime toggle, persists it, and echoes state.
 *
 * The enabled state is read live from [ConfigurationStore] so a toggle takes effect immediately.
 */
@OptIn(ExperimentalKtorApi::class)
class PublicProfileController(
  application: Application,
  buildProfileSnapshotUseCase: BuildProfileSnapshotUseCase,
  configStore: ConfigurationStore,
) {
  init {
    application.routing {
      get<ProfileRoutes> {
          val config = configStore.configuration.publicProfile
          if (!config.enabled) {
            return@get call.respond(HttpStatusCode.NotFound)
          }
          call.respond(
            ProfileSnapshotResource.fromDomain(buildProfileSnapshotUseCase.build(config))
          )
        }
        .describe {
          summary = "Public profile snapshot"
          description =
            """
            Returns the user's public profile (Driver Passport) snapshot, generated from local
            Session/Lap data merged with signup identity. Mirrors the frontend `ProfileData`
            contract and is the same artifact uploaded when cloud publishing is enabled.

            Returns `404 Not Found` when the public profile is disabled.
            """
              .trimIndent()
          responses {
            HttpStatusCode.OK { description = "The profile snapshot." }
            HttpStatusCode.NotFound { description = "The public profile is disabled." }
          }
        }

      put<ProfileRoutes.Enabled> {
          val body = call.receive<PublicProfileEnabled>()
          val updated = configStore.setPublicProfileEnabled(body.enabled)
          call.respond(PublicProfileEnabled(updated.publicProfile.enabled))
        }
        .describe {
          summary = "Toggle the public profile on/off"
          description =
            "Sets whether the public profile is enabled. The new state is persisted to the " +
              "configuration file and reflected immediately in `GET /api/1`."
          responses { HttpStatusCode.OK { description = "The updated enabled state." } }
        }
    }
  }
}
