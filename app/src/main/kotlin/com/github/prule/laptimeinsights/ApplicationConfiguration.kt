package com.github.prule.laptimeinsights

import kotlinx.serialization.Serializable

@Serializable
data class ApplicationConfiguration(
  val port: Int = 8000,
  val clientConfiguration: ApplicationClientConfiguration = ApplicationClientConfiguration(),
  /**
   * Public profile settings. On by default — a fresh install exposes the public profile, which the
   * user can switch off at runtime. The [PublicProfileConfig.enabled] flag is the runtime toggle
   * (persisted back to the config file when flipped); the remaining fields are signup identity that
   * isn't derivable from telemetry.
   */
  val publicProfile: PublicProfileConfig = PublicProfileConfig(),
)

/**
 * Identity + on/off state for the public profile (Driver Passport) page. Identity fields feed the
 * parts of the profile snapshot that can't be computed from local Session/Lap data; everything else
 * in the snapshot is generated from the database.
 */
@Serializable
data class PublicProfileConfig(
  val enabled: Boolean = true,
  val name: String = "Driver",
  val slug: String = "driver",
  val tagline: String = "Sim racer",
  val location: String = "",
  val memberSince: String = "",
  val season: String = "Current Season",
  val sim: String = "Assetto Corsa Competizione",
)
