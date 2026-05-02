package com.github.prule.laptimeinsights.adapter.out.persistence.seed

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
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
import kotlin.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class DatabaseSeederTest : RepositoryTest(listOf(SessionTable, LapTable)) {
  private val sessionMapper = SessionMapper()
  private val lapMapper = LapMapper()
  private val sessionPort =
    SessionPersistenceAdapter(SessionRepository(sessionMapper), sessionMapper)
  private val lapPort = LapPersistenceAdapter(LapRepository(lapMapper), lapMapper)

  private val fixedClock = { Instant.parse("2026-05-03T12:00:00Z") }

  private fun newSeeder() =
    DatabaseSeeder(
      createSessionPort = sessionPort,
      updateSessionPort = sessionPort,
      createLapPort = lapPort,
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
      assertThat(sessions.all { it.isFinished() }).isTrue
      assertThat(laps).isNotEmpty
      assertThat(laps.any { it.personalBest.value }).isTrue
    }
  }

  @Test
  fun `is idempotent — does not double-seed when data exists`() {
    newSeeder().seed()
    val sessionsAfterFirst =
      transaction {
        sessionPort.search(SessionSearchCriteria(), PageRequest(1, 100), Sort.noSort()).total
      }

    newSeeder().seed()
    val sessionsAfterSecond =
      transaction {
        sessionPort.search(SessionSearchCriteria(), PageRequest(1, 100), Sort.noSort()).total
      }

    assertThat(sessionsAfterSecond).isEqualTo(sessionsAfterFirst)
  }
}
