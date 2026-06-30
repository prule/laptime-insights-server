package com.github.prule.laptimeinsights.application.domain.service.profile

import com.github.prule.laptimeinsights.PublicProfileConfig
import com.github.prule.laptimeinsights.application.domain.model.ProfileAggregates
import com.github.prule.laptimeinsights.application.domain.model.ProfileSnapshot
import com.github.prule.laptimeinsights.application.port.`in`.profile.BuildProfileSnapshotUseCase
import com.github.prule.laptimeinsights.application.port.out.profile.ProfileAggregatesPort
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * Builds the public profile snapshot. Numbers come from local [ProfileAggregates]; identity, season
 * label and sim come from the signup [PublicProfileConfig]; presentation (accent palette, track-art
 * key) and a distance estimate are layered on here.
 *
 * `distanceKm` is an **estimate** — the local data has no per-track length, so it is approximated
 * as lap count × a nominal lap length. Documented in docs/public-profile.md.
 */
class BuildProfileSnapshotService(
  private val aggregatesPort: ProfileAggregatesPort,
  private val clock: Clock = Clock.System,
) : BuildProfileSnapshotUseCase {

  override fun build(identity: PublicProfileConfig): ProfileSnapshot {
    val aggregates = transaction { aggregatesPort.loadAggregates() }
    return assemble(identity, aggregates)
  }

  private fun assemble(
    identity: PublicProfileConfig,
    aggregates: ProfileAggregates,
  ): ProfileSnapshot {
    val now = clock.now()
    return ProfileSnapshot(
      meta =
        ProfileSnapshot.Meta(
          slug = identity.slug,
          season = identity.season,
          range = formatRange(aggregates.firstRecordedAt, aggregates.lastRecordedAt),
          generatedAt = now.toString(),
          sim = identity.sim,
        ),
      profile =
        ProfileSnapshot.Profile(
          name = identity.name,
          slug = identity.slug,
          initials = initials(identity.name),
          tagline = identity.tagline,
          location = identity.location,
          memberSince = identity.memberSince,
        ),
      totals =
        ProfileSnapshot.Totals(
          laps = aggregates.playerLapCount,
          distanceKm = Math.round(aggregates.playerLapCount * NOMINAL_LAP_KM),
          hours = round1(aggregates.seatTimeMs / MS_PER_HOUR),
          sessions = aggregates.sessionCount,
          daysActive = aggregates.activeDays.size,
          longestStreak = longestStreak(aggregates.activeDays.map { it.dateKey }),
          tracks = aggregates.trackCount,
          cars = aggregates.carCount,
          topCar = aggregates.topCar ?: "—",
        ),
      perTrack =
        aggregates.perTrackLaps.mapIndexed { i, t ->
          ProfileSnapshot.PerTrack(
            track = t.track,
            laps = t.laps,
            art = t.track,
            accent = ACCENT_PALETTE[i % ACCENT_PALETTE.size],
          )
        },
      records =
        aggregates.records.map { r ->
          ProfileSnapshot.Record(
            track = r.track,
            car = r.car,
            season = formatLapTime(r.seasonBestMs ?: r.allTimeBestMs),
            allTime = formatLapTime(r.allTimeBestMs),
            allTimeWhen = formatMonthYear(r.allTimeWhen),
            isPB = r.seasonBestMs != null && r.seasonBestMs == r.allTimeBestMs,
          )
        },
    )
  }

  companion object {
    /** Estimated average lap length (km) used to approximate total distance. */
    const val NOMINAL_LAP_KM = 5.0
    private const val MS_PER_HOUR = 3_600_000.0
    private val ACCENT_PALETTE =
      listOf("#00d4ff", "#e8212a", "#eab308", "#22c55e", "#f97316", "#a855f7")
    private val RANGE_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy")

    fun initials(name: String): String =
      name
        .trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifEmpty { "?" }

    fun formatLapTime(ms: Long): String {
      val minutes = ms / 60_000
      val seconds = (ms % 60_000) / 1000
      val millis = ms % 1000
      return "%d:%02d.%03d".format(minutes, seconds, millis)
    }

    private fun localDate(instant: Instant) =
      instant.toJavaInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    fun formatMonthYear(instant: Instant): String = localDate(instant).format(MONTH_YEAR_FORMAT)

    fun formatRange(from: Instant?, to: Instant?): String {
      if (from == null || to == null) return ""
      return "${localDate(from).format(RANGE_FORMAT)} – ${localDate(to).format(RANGE_FORMAT)}"
    }

    private fun round1(value: Double): Double = Math.round(value * 10) / 10.0

    /** Longest run of consecutive calendar days among the given `YYYY-MM-DD` keys. */
    fun longestStreak(dayKeys: List<String>): Int {
      if (dayKeys.isEmpty()) return 0
      val days = dayKeys.map { java.time.LocalDate.parse(it) }.toSortedSet().toList()
      var longest = 1
      var current = 1
      for (i in 1 until days.size) {
        current = if (days[i - 1].plusDays(1) == days[i]) current + 1 else 1
        if (current > longest) longest = current
      }
      return longest
    }
  }
}
