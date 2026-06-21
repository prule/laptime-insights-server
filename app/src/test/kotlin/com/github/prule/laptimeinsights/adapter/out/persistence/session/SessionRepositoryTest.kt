package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class SessionRepositoryTest : RepositoryTest(listOf(SessionTable)) {

  private val mapper = SessionMapper()
  private val repository = SessionRepository(mapper)

  @Test
  fun `should create and find session by id`() {
    val session = createTestSession()

    transaction {
      val entity = repository.create(session)
      val foundEntity = repository.findOneOrNull(entity.id.value)

      assertThat(foundEntity).isNotNull
      assertThat(foundEntity?.uid).isEqualTo(session.uid.value)
      assertThat(foundEntity?.simulator).isEqualTo(session.simulator.name)
    }
  }

  @Test
  fun `should persist and read ended_at`() {
    val session = createTestSession(startedAt = Clock.System.now())

    transaction {
      val entity = repository.create(session)
      val createdSession = mapper.toDomain(entity)
      assertThat(createdSession.endedAt()).isNull()

      val endTime = Clock.System.now()
      val endedSession = createdSession.copy().apply { end(endTime) }
      repository.update(endedSession)

      val foundEntity = repository.findOneOrNull(entity.id.value)
      assertThat(foundEntity?.endedAt).isEqualTo(endTime)
      assertThat(mapper.toDomain(foundEntity!!).endedAt()).isEqualTo(endTime)
    }
  }

  @Test
  fun `should search for sessions by criteria`() {
    val session1 = createTestSession(car = Car("Ferrari"))
    val session2 = createTestSession(car = Car("Porsche"))

    transaction {
      repository.create(session1)
      repository.create(session2)

      val criteria = SessionSearchCriteria(car = Car("Ferrari"))
      val result =
        repository.search(
          criteria,
          com.github.prule.laptimeinsights.tracker.utils.data.PageRequest(1),
          com.github.prule.laptimeinsights.tracker.utils.data.Sort.noSort(),
        )

      assertThat(result.items).hasSize(1)
      assertThat(result.items[0].car).isEqualTo("Ferrari")
    }
  }

  @Test
  fun `should update existing session`() {
    val session = createTestSession(startedAt = null)

    transaction {
      val entity = repository.create(session)
      val createdSession = mapper.toDomain(entity)

      val startTime = Clock.System.now()
      val updatedSession = createdSession.copy().apply { start(startTime) }

      repository.update(updatedSession)

      val foundEntity = repository.findOneOrNull(entity.id.value)
      assertThat(foundEntity?.startedAt).isEqualTo(startTime)
    }
  }

  @Test
  fun `should return correct session options`() {
    val session1 = createTestSession(car = Car("Ferrari"), track = Track("Monza"))
    val session2 = createTestSession(car = Car("Porsche"), track = Track("Spa"))

    transaction {
      repository.create(session1)
      repository.create(session2)

      val options = repository.options(SessionSearchCriteria())

      assertThat(options.cars).containsExactlyInAnyOrder(Car("Ferrari"), Car("Porsche"))
      assertThat(options.tracks).containsExactlyInAnyOrder(Track("Monza"), Track("Spa"))
    }
  }

  @Test
  fun `search treats from as inclusive and to as exclusive at the boundaries`() {
    val from = Instant.parse("2026-04-10T00:00:00Z")
    val to = Instant.parse("2026-04-20T00:00:00Z")
    val inside = Instant.parse("2026-04-15T00:00:00Z")

    transaction {
      repository.create(createTestSession(startedAt = from)) // lower bound -> included
      repository.create(createTestSession(startedAt = inside)) // strictly inside -> included
      repository.create(createTestSession(startedAt = to)) // upper bound -> excluded

      val result =
        repository.search(
          SessionSearchCriteria(from = from, to = to),
          com.github.prule.laptimeinsights.tracker.utils.data.PageRequest(1, 25),
          com.github.prule.laptimeinsights.tracker.utils.data.Sort.noSort(),
        )

      assertThat(result.total).isEqualTo(2L)
      assertThat(result.items.map { it.startedAt }).containsExactlyInAnyOrder(from, inside)
    }
  }

  private fun createTestSession(
    car: Car = Car("Ferrari"),
    track: Track = Track("Monza"),
    startedAt: Instant? = Clock.System.now(),
    drivingTime: LapTimeMs = LapTimeMs(0L),
  ) =
    Session(
      id = SessionId(0L), // ID is ignored during creation
      uid = Uid(),
      startedAt = startedAt,
      simulator = Simulator.ACC,
      track = track,
      car = car,
      sessionType = SessionType("Race"),
      drivingTime = drivingTime,
    )

  @Test
  fun `aggregate by day counts sessions and sums driving time per day`() {
    val day1Morning = localTime(2026, 4, 10, 9, 0)
    val day1Evening = localTime(2026, 4, 10, 18, 0)
    val day2 = localTime(2026, 4, 11, 12, 0)
    transaction {
      repository.create(
        createTestSession(startedAt = day1Morning, drivingTime = LapTimeMs(60_000L))
      )
      repository.create(
        createTestSession(startedAt = day1Evening, drivingTime = LapTimeMs(40_000L))
      )
      repository.create(createTestSession(startedAt = day2, drivingTime = LapTimeMs(30_000L)))
      // Session with no startedAt — must be dropped from the aggregate.
      repository.create(createTestSession(startedAt = null, drivingTime = LapTimeMs(99_999L)))

      val buckets =
        repository.aggregate(SessionSearchCriteria(), SessionAggregateGroupBy.DAY).associateBy {
          it.key
        }

      assertThat(buckets).containsOnlyKeys(dayKey(day1Morning), dayKey(day2))
      assertThat(buckets[dayKey(day1Morning)]?.count).isEqualTo(2L)
      assertThat(buckets[dayKey(day1Morning)]?.drivingTimeMs).isEqualTo(100_000L)
      assertThat(buckets[dayKey(day2)]?.count).isEqualTo(1L)
      assertThat(buckets[dayKey(day2)]?.drivingTimeMs).isEqualTo(30_000L)
    }
  }

  @Test
  fun `aggregate by month emits YYYY-MM keys with summed driving time`() {
    val mar = localTime(2026, 3, 15, 12, 0)
    val apr = localTime(2026, 4, 14, 12, 0)
    transaction {
      repository.create(createTestSession(startedAt = mar, drivingTime = LapTimeMs(100_000L)))
      repository.create(createTestSession(startedAt = apr, drivingTime = LapTimeMs(50_000L)))
      repository.create(createTestSession(startedAt = apr, drivingTime = LapTimeMs(70_000L)))

      val buckets =
        repository.aggregate(SessionSearchCriteria(), SessionAggregateGroupBy.MONTH).associateBy {
          it.key
        }

      assertThat(buckets).containsOnlyKeys(monthKey(mar), monthKey(apr))
      assertThat(buckets[monthKey(mar)]?.count).isEqualTo(1L)
      assertThat(buckets[monthKey(mar)]?.drivingTimeMs).isEqualTo(100_000L)
      assertThat(buckets[monthKey(apr)]?.count).isEqualTo(2L)
      assertThat(buckets[monthKey(apr)]?.drivingTimeMs).isEqualTo(120_000L)
    }
  }

  @Test
  fun `aggregate honours from filter`() {
    val anchor = localTime(2026, 4, 15, 12, 0)
    transaction {
      repository.create(
        createTestSession(startedAt = anchor.minus(10.days), drivingTime = LapTimeMs(1L))
      )
      repository.create(
        createTestSession(startedAt = anchor.minus(2.days), drivingTime = LapTimeMs(2L))
      )
      repository.create(createTestSession(startedAt = anchor, drivingTime = LapTimeMs(3L)))

      val buckets =
        repository.aggregate(
          SessionSearchCriteria(from = anchor.minus(7.days)),
          SessionAggregateGroupBy.DAY,
        )

      assertThat(buckets.sumOf { it.count }).isEqualTo(2L)
      assertThat(buckets.sumOf { it.drivingTimeMs }).isEqualTo(5L)
    }
  }

  /** Build an `Instant` for the supplied year/month/day at the local-TZ wall-clock time. */
  private fun localTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant =
    java.time.LocalDateTime.of(year, month, day, hour, minute)
      .atZone(java.time.ZoneId.systemDefault())
      .toInstant()
      .toKotlinInstant()

  private fun dayKey(instant: Instant): String =
    instant.toJavaInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString()

  private fun monthKey(instant: Instant): String =
    instant
      .toJavaInstant()
      .atZone(java.time.ZoneId.systemDefault())
      .toLocalDate()
      .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
}
