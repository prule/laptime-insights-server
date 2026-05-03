package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionMapper
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionRepository
import com.github.prule.laptimeinsights.adapter.out.persistence.session.SessionTable
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
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
        createTestLap(sessionUid = Uid(ferrariSession.uid), sessionId = SessionId(ferrariSession.id.value))
      )
      repository.create(
        createTestLap(sessionUid = Uid(ferrariSession.uid), sessionId = SessionId(ferrariSession.id.value))
      )
      repository.create(
        createTestLap(sessionUid = Uid(porscheSession.uid), sessionId = SessionId(porscheSession.id.value))
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

  private fun buildSession(
    car: Car = Car("Ferrari 488 GT3"),
    track: Track = Track("Monza"),
    simulator: Simulator = Simulator.ACC,
  ) =
    Session(
      id = SessionId(0L),
      uid = Uid(),
      startedAt = Clock.System.now(),
      finishedAt = null,
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
  ) =
    Lap(
      id = LapId(0L),
      uid = Uid(),
      recordedAt = Clock.System.now(),
      carId = CarId(1),
      lapTime = LapTimeMs(90000L),
      lapNumber = LapNumber(1),
      valid = valid,
      personalBest = personalBest,
      sessionId = sessionId,
      sessionUId = sessionUid,
    )
}
