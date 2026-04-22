package com.github.prule.laptimeinsights.adapter.out.persistence.lap

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.tracker.utils.data.PageRequest
import com.github.prule.laptimeinsights.tracker.utils.data.Sort
import kotlin.time.Clock
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.junit.jupiter.api.Test

class LapRepositoryTest : RepositoryTest(listOf(LapTable)) {

  private val mapper = LapMapper()
  private val repository = LapRepository(mapper)

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

  private fun createTestLap(
    sessionId: SessionId = SessionId(1L),
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
      sessionUId = Uid(),
    )
}
