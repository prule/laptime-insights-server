package com.github.prule.laptimeinsights.application.domain.model

/**
 * The public profile snapshot — the artifact the local install serves at `GET
 * /api/1/public-profile` and that a subscriber's install would upload to the cloud. Field groups
 * mirror the frontend `ProfileData` contract: identity/meta from config, everything else generated
 * from local data.
 */
data class ProfileSnapshot(
  val meta: Meta,
  val profile: Profile,
  val totals: Totals,
  val perTrack: List<PerTrack>,
  val records: List<Record>,
) {
  data class Meta(
    val slug: String,
    val season: String,
    val range: String,
    val generatedAt: String,
    val sim: String,
  )

  data class Profile(
    val name: String,
    val slug: String,
    val initials: String,
    val tagline: String,
    val location: String,
    val memberSince: String,
  )

  data class Totals(
    val laps: Long,
    val distanceKm: Long,
    val hours: Double,
    val sessions: Long,
    val daysActive: Int,
    val longestStreak: Int,
    val tracks: Int,
    val cars: Int,
    val topCar: String,
  )

  data class PerTrack(val track: String, val laps: Long, val art: String, val accent: String)

  data class Record(
    val track: String,
    val car: String,
    val season: String,
    val allTime: String,
    val allTimeWhen: String,
    val isPB: Boolean,
  )
}
