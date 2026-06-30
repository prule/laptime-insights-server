package com.github.prule.laptimeinsights.adapter.out.persistence.profile

import com.github.prule.laptimeinsights.PublicProfileConfig
import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTable
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.service.profile.BuildProfileSnapshotService
import java.time.LocalDate
import java.time.ZoneId
import kotlin.time.Instant
import kotlin.time.toKotlinInstant
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

/**
 * Integration test (H2) for the profile snapshot builder: totals, per-track laps,
 * season-vs-all-time records, and invalid-lap exclusion are all derived from local Session/Lap
 * rows.
 */
class ProfileSnapshotServiceTest : RepositoryTest(listOf(LapTable, SessionTable)) {

  private val service = BuildProfileSnapshotService(ProfilePersistenceAdapter())
  private val identity =
    PublicProfileConfig(enabled = true, name = "Ada Lovelace", slug = "ada", season = "2025")

  private val oldYear = Instant.parse("2000-06-01T10:00:00Z")
  private val thisYear =
    LocalDate.now()
      .withMonth(2)
      .withDayOfMonth(1)
      .atStartOfDay(ZoneId.systemDefault())
      .toInstant()
      .toKotlinInstant()

  private fun seed() = transaction {
    SessionTable.insert {
      it[uid] = "s1"
      it[simulator] = "ACC"
      it[sessionType] = "Practice"
      it[track] = "Spa"
      it[car] = "Ferrari"
      it[drivingTimeMs] = 3_600_000L // 1 hour
    }
    insertLap("l1", "Spa", "Ferrari", 95_000, oldYear, valid = true, player = true)
    insertLap("l2", "Spa", "Ferrari", 96_000, thisYear, valid = true, player = true)
    insertLap("l1c", "Spa", "Ferrari", 97_000, thisYear, valid = true, player = true)
    insertLap("l3", "Monza", "Porsche", 90_000, thisYear, valid = true, player = true)
    // Invalid + faster: must NOT win the record, but still counts toward total laps.
    insertLap("l4", "Monza", "Porsche", 80_000, thisYear, valid = false, player = true)
    // Non-player lap: excluded from every player aggregate.
    insertLap("l5", "Spa", "Ferrari", 70_000, thisYear, valid = true, player = false)
  }

  private fun insertLap(
    uidValue: String,
    trackValue: String,
    carValue: String,
    lapTimeValue: Long,
    recordedAtValue: Instant,
    valid: Boolean,
    player: Boolean,
  ) {
    LapTable.insert {
      it[uid] = uidValue
      it[sessionId] = 1
      it[sessionUid] = "s1"
      it[carId] = if (player) 1 else 2
      it[car] = carValue
      it[recordedAt] = recordedAtValue
      it[lapTime] = lapTimeValue
      it[lapNumber] = 1
      it[LapTable.valid] = valid
      it[personalBest] = false
      it[track] = trackValue
      it[playerLap] = player
    }
  }

  @Test
  fun `totals are derived from player laps`() {
    seed()
    val snapshot = service.build(identity)

    assertThat(snapshot.totals.laps).isEqualTo(5) // l1, l2, l1c, l3, l4 (player); l5 excluded
    assertThat(snapshot.totals.sessions).isEqualTo(1)
    assertThat(snapshot.totals.hours).isEqualTo(1.0)
    assertThat(snapshot.totals.tracks).isEqualTo(2)
    assertThat(snapshot.totals.cars).isEqualTo(2)
    assertThat(snapshot.totals.topCar).isEqualTo("Ferrari") // 3 Ferrari player laps vs 2 Porsche
  }

  @Test
  fun `per-track laps reflect player laps`() {
    seed()
    val snapshot = service.build(identity)
    val byTrack = snapshot.perTrack.associate { it.track to it.laps }
    assertThat(byTrack["Spa"]).isEqualTo(3) // l1, l2, l1c
    assertThat(byTrack["Monza"]).isEqualTo(2) // l3, l4
  }

  @Test
  fun `records use valid laps and distinguish season from all-time`() {
    seed()
    val snapshot = service.build(identity)
    val spa = snapshot.records.first { it.track == "Spa" }
    val monza = snapshot.records.first { it.track == "Monza" }

    // Spa all-time best is the 2000 lap; this year's best is slower → not a PB.
    assertThat(spa.allTime).isEqualTo("1:35.000")
    assertThat(spa.season).isEqualTo("1:36.000")
    assertThat(spa.isPB).isFalse()

    // Monza: only a valid lap this year; the faster 80s lap is invalid and excluded.
    assertThat(monza.allTime).isEqualTo("1:30.000")
    assertThat(monza.isPB).isTrue()
  }

  @Test
  fun `disabled-by-default identity still builds when called directly`() {
    // No data seeded — builder must not throw on an empty database.
    val snapshot = service.build(identity)
    assertThat(snapshot.totals.laps).isEqualTo(0)
    assertThat(snapshot.perTrack).isEmpty()
    assertThat(snapshot.profile.initials).isEqualTo("AL")
  }
}
