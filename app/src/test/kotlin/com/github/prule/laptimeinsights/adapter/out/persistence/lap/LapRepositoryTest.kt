package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.AllTimeBest
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.LapAggregateGroupBy
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.PlayerLap
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class LapRepositoryTest : RepositoryTest(listOf(LapTable, SessionTable)) {

  private val mapper = LapMapper()
  private val repository = LapRepository(mapper)
  private val sessionMapper = SessionMapper()
  private val sessionRepository = SessionRepository(sessionMapper)

  @Test
  fun `should create and find lap by id`() {
    val lap = createTestLap()

    transaction {
      val entity = repository.create(lap)
      val foundEntity = repository.findOneOrNull(entity.id.value)

      assertThat(foundEntity).isNotNull
      assertThat(foundEntity?.uid).isEqualTo(lap.uid.value)
      assertThat(foundEntity?.lapTime).isEqualTo(lap.lapTime.value)
    }
  }

  @Test
  fun `should search for laps by session id`() {
    val sessionId1 = SessionId(100L)
    val sessionId2 = SessionId(200L)
    val lap1 = createTestLap(sessionId = sessionId1)
    val lap2 = createTestLap(sessionId = sessionId2)

    transaction {
      repository.create(lap1)
      repository.create(lap2)

      val criteria = LapSearchCriteria(sessionId = sessionId1)
      val result = repository.search(criteria, PageRequest(1), Sort.noSort())

      assertThat(result.items).hasSize(1)
      assertThat(result.items[0].sessionId).isEqualTo(100L)
    }
  }

  @Test
  fun `should search for personal best laps`() {
    val lap1 = createTestLap(personalBest = PersonalBest(true))
    val lap2 = createTestLap(personalBest = PersonalBest(false))

    transaction {
      repository.create(lap1)
      repository.create(lap2)

      val criteria = LapSearchCriteria(personalBest = PersonalBest(true))
      val result = repository.search(criteria, PageRequest(1), Sort.noSort())

      assertThat(result.items).hasSize(1)
      assertThat(result.items[0].personalBest).isTrue()
    }
  }

  @Test
  fun `should search for player laps only`() {
    val playerLap = createTestLap(playerLap = true)
    val competitorLap = createTestLap(playerLap = false)
    val nullLap = createTestLap(playerLap = null)

    transaction {
      repository.create(playerLap)
      repository.create(competitorLap)
      repository.create(nullLap)

      val result =
        repository.search(
          LapSearchCriteria(playerLap = PlayerLap(true)),
          PageRequest(1),
          Sort.noSort(),
        )

      assertThat(result.items).hasSize(1)
      assertThat(result.items[0].playerLap).isTrue()
    }
  }

  @Test
  fun `should search for valid laps`() {
    val lap1 = createTestLap(valid = ValidLap(true))
    val lap2 = createTestLap(valid = ValidLap(false))

    transaction {
      repository.create(lap1)
      repository.create(lap2)

      val criteria = LapSearchCriteria(validLap = ValidLap(true))
      val result = repository.search(criteria, PageRequest(1), Sort.noSort())

      assertThat(result.items).hasSize(1)
      assertThat(result.items[0].valid).isTrue()
    }
  }

  @Test
  fun `should search for laps by session uid`() {
    val sessionUid1 = Uid()
    val sessionUid2 = Uid()
    val lap1 = createTestLap(sessionUid = sessionUid1)
    val lap2 = createTestLap(sessionUid = sessionUid2)

    transaction {
      repository.create(lap1)
      repository.create(lap2)

      val criteria = LapSearchCriteria(sessionUid = sessionUid1)
      val result = repository.search(criteria, PageRequest(1), Sort.noSort())

      assertThat(result.items).hasSize(1)
      assertThat(result.items[0].sessionUid).isEqualTo(sessionUid1.value)
    }
  }

  @Test
  fun `should update existing lap`() {
    val lap = createTestLap(valid = ValidLap(true))

    transaction {
      val entity = repository.create(lap)
      val createdLap = mapper.toDomain(entity)

      val updatedLap = createdLap.copy(valid = ValidLap(false))
      repository.update(updatedLap)

      val foundEntity = repository.findOneOrNull(entity.id.value)
      assertThat(foundEntity?.valid).isFalse()
    }
  }

  @Test
  fun `should filter laps by owning session car`() {
    transaction {
      val ferrariSession = sessionRepository.create(buildSession(car = Car("Ferrari 488 GT3")))
      val porscheSession = sessionRepository.create(buildSession(car = Car("Porsche 991 II GT3 R")))

      repository.create(
        createTestLap(
          sessionUid = Uid(ferrariSession.uid),
          sessionId = SessionId(ferrariSession.id.value),
        )
      )
      repository.create(
        createTestLap(
          sessionUid = Uid(ferrariSession.uid),
          sessionId = SessionId(ferrariSession.id.value),
        )
      )
      repository.create(
        createTestLap(
          sessionUid = Uid(porscheSession.uid),
          sessionId = SessionId(porscheSession.id.value),
        )
      )

      val result =
        repository.search(
          LapSearchCriteria(car = Car("Ferrari 488 GT3")),
          PageRequest(1, 25),
          Sort.noSort(),
        )

      assertThat(result.total).isEqualTo(2L)
      assertThat(result.items).hasSize(2)
      assertThat(result.items.map { it.sessionUid }).allMatch { it == ferrariSession.uid }
    }
  }

  @Test
  fun `should filter laps by owning session track and simulator`() {
    transaction {
      val accSpa =
        sessionRepository.create(buildSession(track = Track("Spa"), simulator = Simulator.ACC))
      val accMonza =
        sessionRepository.create(buildSession(track = Track("Monza"), simulator = Simulator.ACC))
      val f1Monza =
        sessionRepository.create(buildSession(track = Track("Monza"), simulator = Simulator.F1))

      repository.create(createTestLap(sessionUid = Uid(accSpa.uid)))
      repository.create(createTestLap(sessionUid = Uid(accMonza.uid)))
      repository.create(createTestLap(sessionUid = Uid(f1Monza.uid)))

      val byTrack =
        repository.search(
          LapSearchCriteria(track = Track("Monza")),
          PageRequest(1, 25),
          Sort.noSort(),
        )
      assertThat(byTrack.total).isEqualTo(2L)

      val byTrackAndSim =
        repository.search(
          LapSearchCriteria(track = Track("Monza"), simulator = Simulator.F1),
          PageRequest(1, 25),
          Sort.noSort(),
        )
      assertThat(byTrackAndSim.total).isEqualTo(1L)
      assertThat(byTrackAndSim.items[0].sessionUid).isEqualTo(f1Monza.uid)
    }
  }

  @Test
  fun `should filter laps by recordedAt range`() {
    val anchor = Instant.parse("2026-04-15T12:00:00Z")
    val tenDaysAgo = anchor.minus(10.days)
    val fiveDaysAgo = anchor.minus(5.days)
    val now = anchor

    transaction {
      repository.create(createTestLap(recordedAt = tenDaysAgo))
      repository.create(createTestLap(recordedAt = fiveDaysAgo))
      repository.create(createTestLap(recordedAt = now))

      val onlyLastWeek =
        repository.search(
          LapSearchCriteria(from = anchor.minus(7.days)),
          PageRequest(1, 25),
          Sort.noSort(),
        )
      assertThat(onlyLastWeek.total).isEqualTo(2L)

      val between =
        repository.search(
          LapSearchCriteria(from = anchor.minus(7.days), to = anchor.minus(1.days)),
          PageRequest(1, 25),
          Sort.noSort(),
        )
      assertThat(between.total).isEqualTo(1L)
      assertThat(between.items[0].recordedAt).isEqualTo(fiveDaysAgo)
    }
  }

  private fun buildSession(
    car: Car = Car("Ferrari 488 GT3"),
    track: Track = Track("Monza"),
    simulator: Simulator = Simulator.ACC,
  ) =
    Session(
      id = SessionId(0L),
      uid = Uid(),
      startedAt = Clock.System.now(),
      simulator = simulator,
      track = track,
      car = car,
      sessionType = SessionType("Race"),
    )

  private fun createTestLap(
    sessionId: SessionId = SessionId(1L),
    sessionUid: Uid = Uid(),
    personalBest: PersonalBest = PersonalBest(false),
    valid: ValidLap = ValidLap(true),
    recordedAt: Instant = Clock.System.now(),
    playerLap: Boolean? = true,
    lapTime: LapTimeMs = LapTimeMs(90000L),
    track: Track? = Track("testTrack"),
  ) =
    Lap(
      id = LapId(0L),
      uid = Uid(),
      recordedAt = recordedAt,
      carId = CarId(1),
      lapTime = lapTime,
      lapNumber = LapNumber(1),
      valid = valid,
      personalBest = personalBest,
      sessionId = sessionId,
      sessionUId = sessionUid,
      car = Car("testCar"),
      track = track,
      playerLap = playerLap,
    )

  @Test
  fun `allTimeBest keeps fastest valid player lap per track`() {
    transaction {
      // Spa: three player laps, fastest is 90s
      repository.create(
        createTestLap(track = Track("Spa"), lapTime = LapTimeMs(95_000L), playerLap = true)
      )
      repository.create(
        createTestLap(track = Track("Spa"), lapTime = LapTimeMs(90_000L), playerLap = true)
      )
      repository.create(
        createTestLap(track = Track("Spa"), lapTime = LapTimeMs(92_000L), playerLap = true)
      )
      // Monza: two player laps, fastest is 88s
      repository.create(
        createTestLap(track = Track("Monza"), lapTime = LapTimeMs(88_000L), playerLap = true)
      )
      repository.create(
        createTestLap(track = Track("Monza"), lapTime = LapTimeMs(89_000L), playerLap = true)
      )
      // Imola: a faster competitor lap that should be excluded by playerLap=true
      repository.create(
        createTestLap(track = Track("Imola"), lapTime = LapTimeMs(80_000L), playerLap = false)
      )
      repository.create(
        createTestLap(track = Track("Imola"), lapTime = LapTimeMs(91_000L), playerLap = true)
      )
      // Spa: an invalid lap that's "faster" — must not become the best
      repository.create(
        createTestLap(
          track = Track("Spa"),
          lapTime = LapTimeMs(50_000L),
          playerLap = true,
          valid = ValidLap(false),
        )
      )
      // Null-track lap — must be dropped entirely (no track to attribute the best to)
      repository.create(createTestLap(track = null, lapTime = LapTimeMs(70_000L), playerLap = true))

      val result =
        repository.search(
          LapSearchCriteria(
            playerLap = PlayerLap(true),
            validLap = ValidLap(true),
            allTimeBest = AllTimeBest(true),
          ),
          PageRequest(1, 25),
          Sort.noSort(),
        )

      assertThat(result.total).isEqualTo(3L)
      val byTrack = result.items.associateBy({ it.track }, { it.lapTime })
      assertThat(byTrack["Spa"]).isEqualTo(90_000L)
      assertThat(byTrack["Monza"]).isEqualTo(88_000L)
      assertThat(byTrack["Imola"]).isEqualTo(91_000L)
    }
  }

  @Test
  fun `allTimeBest with track sort orders by track name`() {
    transaction {
      repository.create(createTestLap(track = Track("Zandvoort"), lapTime = LapTimeMs(95_000L)))
      repository.create(createTestLap(track = Track("Brands Hatch"), lapTime = LapTimeMs(80_000L)))
      repository.create(createTestLap(track = Track("Monza"), lapTime = LapTimeMs(85_000L)))

      val result =
        repository.search(
          LapSearchCriteria(
            playerLap = PlayerLap(true),
            validLap = ValidLap(true),
            allTimeBest = AllTimeBest(true),
          ),
          PageRequest(1, 25),
          com.github.prule.laptimeinsights.tracker.utils.data.Sort(
            listOf(
              com.github.prule.laptimeinsights.tracker.utils.data.SortBy(
                "track",
                com.github.prule.laptimeinsights.tracker.utils.data.Order.ASC,
              )
            )
          ),
        )

      assertThat(result.items.map { it.track })
        .containsExactly("Brands Hatch", "Monza", "Zandvoort")
    }
  }

  @Test
  fun `aggregate by track counts player laps per track and drops null-track rows`() {
    transaction {
      repeat(3) { repository.create(createTestLap(track = Track("Spa"), playerLap = true)) }
      repeat(2) { repository.create(createTestLap(track = Track("Monza"), playerLap = true)) }
      // Competitor laps should be excluded when filtering playerLap=true.
      repository.create(createTestLap(track = Track("Spa"), playerLap = false))
      // Null-track lap must not appear in any bucket.
      repository.create(createTestLap(track = null, playerLap = true))

      val buckets =
        repository.aggregate(
          LapSearchCriteria(playerLap = PlayerLap(true)),
          LapAggregateGroupBy.TRACK,
        )

      val byTrack = buckets.associateBy({ it.key }, { it.count })
      assertThat(byTrack).containsOnlyKeys("Spa", "Monza")
      assertThat(byTrack["Spa"]).isEqualTo(3L)
      assertThat(byTrack["Monza"]).isEqualTo(2L)
    }
  }

  @Test
  fun `aggregate by day groups laps by recordedAt date`() {
    // Anchor times at local midday so they fall on the same wall-clock day in every reasonable TZ;
    // expected keys are derived from the same system zone the repo uses so the test is portable.
    val day1Morning = localTime(2026, 4, 10, 9, 0)
    val day1Evening = localTime(2026, 4, 10, 18, 0)
    val day2 = localTime(2026, 4, 11, 12, 0)
    val day3 = localTime(2026, 4, 12, 12, 0)
    transaction {
      repository.create(createTestLap(recordedAt = day1Morning))
      repository.create(createTestLap(recordedAt = day1Evening))
      repository.create(createTestLap(recordedAt = day2))
      repository.create(createTestLap(recordedAt = day3))

      val buckets =
        repository.aggregate(LapSearchCriteria(), LapAggregateGroupBy.DAY).associateBy(
          { it.key },
          { it.count },
        )

      assertThat(buckets).containsOnlyKeys(dayKey(day1Morning), dayKey(day2), dayKey(day3))
      assertThat(buckets[dayKey(day1Morning)]).isEqualTo(2L)
      assertThat(buckets[dayKey(day2)]).isEqualTo(1L)
      assertThat(buckets[dayKey(day3)]).isEqualTo(1L)
    }
  }

  @Test
  fun `aggregate by month emits YYYY-MM keys`() {
    // Mid-month anchors are TZ-stable; expected keys derived from system zone.
    val mar = localTime(2026, 3, 15, 12, 0)
    val apr1 = localTime(2026, 4, 14, 12, 0)
    val apr2 = localTime(2026, 4, 28, 12, 0)
    transaction {
      repository.create(createTestLap(recordedAt = mar))
      repository.create(createTestLap(recordedAt = apr1))
      repository.create(createTestLap(recordedAt = apr2))

      val buckets =
        repository
          .aggregate(LapSearchCriteria(), LapAggregateGroupBy.MONTH)
          .associateBy({ it.key }, { it.count })

      assertThat(buckets).containsOnlyKeys(monthKey(mar), monthKey(apr1))
      assertThat(buckets[monthKey(apr1)]).isEqualTo(2L)
      assertThat(buckets[monthKey(mar)]).isEqualTo(1L)
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

  @Test
  fun `aggregate honours from and to filters`() {
    val anchor = Instant.parse("2026-04-15T12:00:00Z")
    transaction {
      repository.create(createTestLap(recordedAt = anchor.minus(10.days)))
      repository.create(createTestLap(recordedAt = anchor.minus(2.days)))
      repository.create(createTestLap(recordedAt = anchor))

      val buckets =
        repository.aggregate(
          LapSearchCriteria(from = anchor.minus(7.days)),
          LapAggregateGroupBy.DAY,
        )

      assertThat(buckets.sumOf { it.count }).isEqualTo(2L)
    }
  }
}
