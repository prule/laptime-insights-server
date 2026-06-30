package com.github.prule.laptimeinsights.adapter.`in`.web.profile

import com.github.prule.laptimeinsights.application.domain.model.ProfileSnapshot
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shape of the public profile snapshot — mirrors the frontend `ProfileData` contract field for
 * field, so the same JSON drives the page and (later) the cloud upload.
 */
@Serializable
data class ProfileSnapshotResource(
  val meta: Meta,
  val profile: Profile,
  val totals: Totals,
  val perTrack: List<PerTrack>,
  val records: List<Record>,
) {
  @Serializable
  data class Meta(
    val slug: String,
    val season: String,
    val range: String,
    val generatedAt: String,
    val sim: String,
  )

  @Serializable
  data class Profile(
    val name: String,
    val slug: String,
    val initials: String,
    val tagline: String,
    val location: String,
    @SerialName("member_since") val memberSince: String,
  )

  @Serializable
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

  @Serializable
  data class PerTrack(val track: String, val laps: Long, val art: String, val accent: String)

  @Serializable
  data class Record(
    val track: String,
    val car: String,
    val season: String,
    val allTime: String,
    val allTimeWhen: String,
    val isPB: Boolean,
  )

  companion object {
    fun fromDomain(s: ProfileSnapshot): ProfileSnapshotResource =
      ProfileSnapshotResource(
        meta =
          Meta(
            slug = s.meta.slug,
            season = s.meta.season,
            range = s.meta.range,
            generatedAt = s.meta.generatedAt,
            sim = s.meta.sim,
          ),
        profile =
          Profile(
            name = s.profile.name,
            slug = s.profile.slug,
            initials = s.profile.initials,
            tagline = s.profile.tagline,
            location = s.profile.location,
            memberSince = s.profile.memberSince,
          ),
        totals =
          Totals(
            laps = s.totals.laps,
            distanceKm = s.totals.distanceKm,
            hours = s.totals.hours,
            sessions = s.totals.sessions,
            daysActive = s.totals.daysActive,
            longestStreak = s.totals.longestStreak,
            tracks = s.totals.tracks,
            cars = s.totals.cars,
            topCar = s.totals.topCar,
          ),
        perTrack =
          s.perTrack.map {
            PerTrack(track = it.track, laps = it.laps, art = it.art, accent = it.accent)
          },
        records =
          s.records.map {
            Record(
              track = it.track,
              car = it.car,
              season = it.season,
              allTime = it.allTime,
              allTimeWhen = it.allTimeWhen,
              isPB = it.isPB,
            )
          },
      )
  }
}

/** Body + response for the toggle action `PUT /api/1/public-profile/enabled`. */
@Serializable data class PublicProfileEnabled(val enabled: Boolean)
