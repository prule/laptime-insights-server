package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionCreated
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.CreateSessionCommand
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.CreateSessionPort
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CreateSessionServiceTest {

  private val createSessionPort = mockk<CreateSessionPort>()
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service = CreateSessionService(createSessionPort, eventPort)

  @Test
  fun `should create session, persist, and emit SessionCreated event`() {
    val command =
      CreateSessionCommand(
        simulator = Simulator.ACC,
        sessionType = SessionType("Race"),
        track = Track("Monza"),
        car = Car("Ferrari 296 GT3"),
      )
    val persistedUid = Uid()
    val persistedId = SessionId(42L)
    val sessionSlot = slot<Session>()

    every { createSessionPort.create(capture(sessionSlot)) } answers
      {
        sessionSlot.captured.copy(id = persistedId, uid = persistedUid)
      }
    every { eventPort.emit(any()) } returns Unit

    val result = service.createSession(command)

    // Result reflects the persisted session, not the in-memory one passed to the port.
    assertThat(result.id).isEqualTo(persistedId)
    assertThat(result.uid).isEqualTo(persistedUid)
    assertThat(result.simulator).isEqualTo(Simulator.ACC)
    assertThat(result.sessionType).isEqualTo(SessionType("Race"))
    assertThat(result.track).isEqualTo(Track("Monza"))
    assertThat(result.car).isEqualTo(Car("Ferrari 296 GT3"))
    assertThat(result.startedAt()).isNull()
    assertThat(result.finishedAt()).isNull()

    // Service constructed a fresh in-memory session with a generated UID and no times set.
    assertThat(sessionSlot.captured.id).isEqualTo(SessionId(0))
    assertThat(sessionSlot.captured.uid.value).isNotBlank()
    assertThat(sessionSlot.captured.startedAt()).isNull()
    assertThat(sessionSlot.captured.finishedAt()).isNull()

    // Event carries the persisted session (with the real id/uid), not the pre-persistence one.
    verify {
      eventPort.emit(
        match { it is SessionCreated && it.session.uid == persistedUid && it.session.id == persistedId }
      )
    }
  }
}
