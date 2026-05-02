package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapCreated
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapTimeMs
import com.github.prule.laptimeinsights.application.domain.model.PersonalBest
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.domain.model.ValidLap
import com.github.prule.laptimeinsights.application.port.`in`.lap.CreateLapCommand
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.lap.CreateLapPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlin.time.Clock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class CreateLapServiceTest {

  private val createLapPort = mockk<CreateLapPort>()
  private val searchSessionPort = mockk<SearchSessionPort>()
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service = CreateLapService(createLapPort, searchSessionPort, eventPort)

  private val sessionId = SessionId(42L)
  private val sessionUid = Uid()
  private val existingSession =
    Session(
      id = sessionId,
      uid = sessionUid,
      startedAt = Clock.System.now(),
      finishedAt = null,
      simulator = Simulator.ACC,
      track = Track("Spa"),
      car = Car("Ferrari"),
      sessionType = SessionType("Race"),
    )

  @Test
  fun `should create lap, populate session id and uid, and emit LapCreated event`() {
    val command =
      CreateLapCommand(
        sessionUid = sessionUid,
        recordedAt = Clock.System.now(),
        carId = CarId(7),
        lapTime = LapTimeMs(96_745L),
        lapNumber = LapNumber(3),
        valid = ValidLap(true),
        personalBest = PersonalBest(false),
      )
    val persistedLapId = LapId(100L)
    val persistedLapUid = Uid()
    val lapSlot = slot<Lap>()

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { createLapPort.create(capture(lapSlot)) } answers
      {
        lapSlot.captured.copy(id = persistedLapId, uid = persistedLapUid)
      }
    every { eventPort.emit(any()) } returns Unit

    val result = service.createLap(command)

    // Service resolved the session and stamped its id/uid onto the new lap before persisting.
    assertThat(lapSlot.captured.sessionId).isEqualTo(sessionId)
    assertThat(lapSlot.captured.sessionUId).isEqualTo(sessionUid)
    assertThat(lapSlot.captured.carId).isEqualTo(CarId(7))
    assertThat(lapSlot.captured.lapTime).isEqualTo(LapTimeMs(96_745L))
    assertThat(lapSlot.captured.lapNumber).isEqualTo(LapNumber(3))
    assertThat(lapSlot.captured.valid).isEqualTo(ValidLap(true))
    assertThat(lapSlot.captured.personalBest).isEqualTo(PersonalBest(false))

    // Result reflects the persisted lap (with the real id/uid).
    assertThat(result.id).isEqualTo(persistedLapId)
    assertThat(result.uid).isEqualTo(persistedLapUid)

    // Event carries the persisted lap.
    verify {
      eventPort.emit(
        match { it is LapCreated && it.lap.id == persistedLapId && it.lap.uid == persistedLapUid }
      )
    }
  }

  @Test
  fun `should throw NotFoundException when session does not exist`() {
    val command =
      CreateLapCommand(
        sessionUid = Uid(),
        recordedAt = Clock.System.now(),
        carId = CarId(1),
        lapTime = LapTimeMs(90_000L),
        lapNumber = LapNumber(1),
        valid = ValidLap(true),
        personalBest = PersonalBest(false),
      )

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    assertThatThrownBy { service.createLap(command) }
      .isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `should not emit event when session does not exist`() {
    val command =
      CreateLapCommand(
        sessionUid = Uid(),
        recordedAt = Clock.System.now(),
        carId = CarId(1),
        lapTime = LapTimeMs(90_000L),
        lapNumber = LapNumber(1),
        valid = ValidLap(true),
        personalBest = PersonalBest(false),
      )

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    runCatching { service.createLap(command) }

    verify(exactly = 0) { eventPort.emit(any()) }
  }
}
