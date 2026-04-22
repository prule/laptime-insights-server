package com.github.prule.laptimeinsights.adapter.out.persistence.session

import com.github.prule.laptimeinsights.adapter.out.persistence.RepositoryTest
import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import kotlin.time.Clock
import kotlin.time.Instant
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

  private fun createTestSession(
    car: Car = Car("Ferrari"),
    track: Track = Track("Monza"),
    startedAt: Instant? = Clock.System.now(),
  ) =
    Session(
      id = SessionId(0L), // ID is ignored during creation
      uid = Uid(),
      startedAt = startedAt,
      finishedAt = null,
      simulator = Simulator.ACC,
      track = track,
      car = car,
      sessionType = SessionType("Race"),
    )
}
