package com.github.prule.laptimeinsights.adapter.out.persistence.seed

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.adapter.out.persistence.car.RealtimeCarUpdatePersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.car.RealtimeCarUpdateRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.car.RealtimeCarUpdateTable
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.lap.LapTable
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionPersistenceAdapter
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class DatabaseSeederTest : RepositoryTest(listOf(SessionTable, LapTable, RealtimeCarUpdateTable)) {

  private val sessionMapper = SessionMapper()
  private val lapMapper = LapMapper()
  private val sessionPort =
    SessionPersistenceAdapter(SessionRepository(sessionMapper), sessionMapper)
  private val lapPort = LapPersistenceAdapter(LapRepository(lapMapper), lapMapper)
  private val realtimeCarUpdatePort =
    RealtimeCarUpdatePersistenceAdapter(RealtimeCarUpdateRepository())

  private val fixedClock = { Instant.parse("2026-05-03T12:00:00Z") }

  private fun newSeeder() =
    DatabaseSeeder(
      createSessionPort = sessionPort,
      updateSessionPort = sessionPort,
      createLapPort = lapPort,
      createRealtimeCarUpdatePort = realtimeCarUpdatePort,
      clock = fixedClock,
      random = Random(42),
    )

  @Test
  fun `seeds sessions across multiple cars, tracks and dates`() {
    newSeeder().seed()

    transaction {
      val sessions =
        sessionPort.search(SessionSearchCriteria(), PageRequest(1, 100), Sort.noSort()).items
      val laps = lapPort.search(LapSearchCriteria(), PageRequest(1, 1000), Sort.noSort()).items

      assertThat(sessions).isNotEmpty
      assertThat(sessions.map { it.car?.value }.toSet()).hasSizeGreaterThan(3)
      assertThat(sessions.map { it.track?.value }.toSet()).hasSizeGreaterThan(3)
      assertThat(sessions.map { it.simulator }.toSet()).hasSizeGreaterThan(1)
      assertThat(sessions.mapNotNull { it.startedAt() }.toSet()).hasSizeGreaterThan(3)
      // Player-car laps should have folded into each session's drivingTime aggregate.
      assertThat(sessions.all { it.drivingTime().value > 0L }).isTrue
      assertThat(laps).isNotEmpty
      assertThat(laps.any { it.personalBest.value }).isTrue
    }
  }

  @Test
  fun `seeds realtime car update samples for every lap`() {
    newSeeder().seed()

    transaction {
      val laps = lapPort.search(LapSearchCriteria(), PageRequest(1, 1000), Sort.noSort()).items
      assertThat(laps).isNotEmpty
      // Spot-check first lap: samples ordered, monotonically increasing
      // splinePosition, plausible telemetry values.
      val first = laps.first()
      val samples = realtimeCarUpdatePort.findByLapUid(first.uid)
      assertThat(samples).hasSizeGreaterThan(50)
      assertThat(samples.first().splinePosition).isLessThan(samples.last().splinePosition)
      assertThat(samples).allSatisfy {
        assertThat(it.splinePosition).isBetween(0.0, 1.0)
        assertThat(it.speedKph).isPositive()
        assertThat(it.gear).isBetween(1, 7)
      }
    }
  }

  @Test
  fun `seeds sessions across every dashboard time-range bucket`() {
    newSeeder().seed()

    transaction {
      val now = fixedClock()
      val sessions =
        sessionPort
          .search(SessionSearchCriteria(), PageRequest(1, 100), Sort.noSort())
          .items
          .mapNotNull { it.startedAt() }

      val inOneMonth = sessions.count { it >= now - 30.days }
      val inThreeMonths = sessions.count { it >= now - 90.days }
      val inSixMonths = sessions.count { it >= now - 180.days }
      val inOneYear = sessions.count { it >= now - 365.days }
      val all = sessions.size

      assertThat(inOneMonth).isGreaterThan(0)
      assertThat(inThreeMonths).isGreaterThan(inOneMonth)
      assertThat(inSixMonths).isGreaterThan(inThreeMonths)
      assertThat(inOneYear).isGreaterThan(inSixMonths)
      assertThat(all).isGreaterThan(inOneYear)
    }
  }

  @Test
  fun `is idempotent — does not double-seed when data exists`() {
    newSeeder().seed()
    val sessionsAfterFirst = transaction {
      sessionPort.search(SessionSearchCriteria(), PageRequest(1, 100), Sort.noSort()).total
    }

    newSeeder().seed()
    val sessionsAfterSecond = transaction {
      sessionPort.search(SessionSearchCriteria(), PageRequest(1, 100), Sort.noSort()).total
    }

    assertThat(sessionsAfterSecond).isEqualTo(sessionsAfterFirst)
  }
}
