package com.github.prule.laptimeinsights.application.domain.service.lap

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.CarId
import com.github.prule.laptimeinsights.application.domain.model.Lap
import com.github.prule.laptimeinsights.application.domain.model.LapCreated
import com.github.prule.laptimeinsights.application.domain.model.LapId
import com.github.prule.laptimeinsights.application.domain.model.LapNumber
import com.github.prule.laptimeinsights.application.domain.model.LapSearchCriteria
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
import com.github.prule.laptimeinsights.application.port.out.lap.SearchLapPort
import com.github.prule.laptimeinsights.application.port.out.lap.UpdateLapPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
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
  private val searchLapPort = mockk<SearchLapPort>()
  private val updateLapPort = mockk<UpdateLapPort>()
  private val searchSessionPort = mockk<SearchSessionPort>()
  private val updateSessionPort = mockk<UpdateSessionPort>(relaxed = true)
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service =
    CreateLapService(
      createLapPort,
      searchLapPort,
      updateLapPort,
      searchSessionPort,
      updateSessionPort,
      eventPort,
    )

  private val sessionId = SessionId(42L)
  private val sessionUid = Uid()
  private val playerCarId = CarId(7)

  private fun session(carId: CarId? = playerCarId) =
    Session(
      id = sessionId,
      uid = sessionUid,
      startedAt = Clock.System.now(),
      simulator = Simulator.ACC,
      track = Track("Spa"),
      car = Car("Ferrari"),
      sessionType = SessionType("Race"),
      playerCarId = carId,
    )

  private fun command(
    lapTimeMs: Long,
    lapNumber: Int,
    valid: Boolean = true,
    carId: Int = playerCarId.value,
  ) =
    CreateLapCommand(
      sessionUid = sessionUid,
      recordedAt = Clock.System.now(),
      carId = CarId(carId),
      car = Car("Audi R8 LMS Evo"),
      lapTime = LapTimeMs(lapTimeMs),
      lapNumber = LapNumber(lapNumber),
      valid = ValidLap(valid),
    )

  @Test
  fun `should create lap, populate session id and uid, and emit LapCreated event`() {
    val cmd = command(lapTimeMs = 96_745L, lapNumber = 3)
    val persistedLapId = LapId(100L)
    val persistedLapUid = Uid()
    val lapSlot = slot<Lap>()

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(capture(lapSlot)) } answers
      {
        lapSlot.captured.copy(id = persistedLapId, uid = persistedLapUid)
      }
    // No prior PB → new lap becomes PB.
    every { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) } returns null
    every { updateLapPort.update(any()) } answers { firstArg() }
    every { eventPort.emit(any()) } returns Unit

    val result = service.createLap(cmd)

    // Service resolved the session and stamped its id/uid onto the new lap before persisting.
    assertThat(lapSlot.captured.sessionId).isEqualTo(sessionId)
    assertThat(lapSlot.captured.sessionUId).isEqualTo(sessionUid)
    assertThat(lapSlot.captured.carId).isEqualTo(playerCarId)
    assertThat(lapSlot.captured.car).isEqualTo(Car("Audi R8 LMS Evo"))
    assertThat(lapSlot.captured.lapTime).isEqualTo(LapTimeMs(96_745L))
    assertThat(lapSlot.captured.lapNumber).isEqualTo(LapNumber(3))
    assertThat(lapSlot.captured.valid).isEqualTo(ValidLap(true))
    // Always persisted as non-PB initially; PB is set via the update port.
    assertThat(lapSlot.captured.personalBest).isEqualTo(PersonalBest(false))

    // Result reflects the persisted lap (with the real id/uid) and is now PB because no prior
    // PB existed for this session+car.
    assertThat(result.id).isEqualTo(persistedLapId)
    assertThat(result.uid).isEqualTo(persistedLapUid)
    assertThat(result.personalBest).isEqualTo(PersonalBest(true))

    // Event carries the persisted (PB-promoted) lap.
    verify {
      eventPort.emit(
        match {
          it is LapCreated &&
            it.lap.id == persistedLapId &&
            it.lap.uid == persistedLapUid &&
            it.lap.personalBest == PersonalBest(true)
        }
      )
    }
  }

  @Test
  fun `should fold player car lap time into session drivingTime aggregate`() {
    val cmd = command(lapTimeMs = 96_745L, lapNumber = 3)
    val sessionSlot = slot<Session>()

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = LapId(1), uid = Uid())
      }
    every { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) } returns null
    every { updateLapPort.update(any()) } answers { firstArg() }
    every { updateSessionPort.update(capture(sessionSlot)) } answers { firstArg() }

    service.createLap(cmd)

    // Player-car lap → session drivingTime now includes this lap's time.
    assertThat(sessionSlot.captured.drivingTime()).isEqualTo(LapTimeMs(96_745L))
  }

  @Test
  fun `invalid player car lap still counts toward drivingTime`() {
    // drivingTime = "time the player spent on track", not "time spent on valid laps".
    val cmd = command(lapTimeMs = 80_000L, lapNumber = 5, valid = false)
    val sessionSlot = slot<Session>()

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = LapId(1), uid = Uid())
      }
    every { updateSessionPort.update(capture(sessionSlot)) } answers { firstArg() }

    service.createLap(cmd)

    assertThat(sessionSlot.captured.drivingTime()).isEqualTo(LapTimeMs(80_000L))
  }

  @Test
  fun `competitor lap does not touch session drivingTime`() {
    // Competitor (carId != playerCarId) → session left alone.
    val cmd = command(lapTimeMs = 96_745L, lapNumber = 3, carId = 99)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = LapId(1), uid = Uid())
      }
    every { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) } returns null
    every { updateLapPort.update(any()) } answers { firstArg() }

    service.createLap(cmd)

    verify(exactly = 0) { updateSessionPort.update(any()) }
  }

  @Test
  fun `lap on session without playerCarId set does not touch session`() {
    // Pre-EntryListCar: no player car known, so we can't attribute laps yet.
    val cmd = command(lapTimeMs = 96_745L, lapNumber = 3)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns
      session(carId = null)
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = LapId(1), uid = Uid())
      }
    every { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) } returns null
    every { updateLapPort.update(any()) } answers { firstArg() }

    service.createLap(cmd)

    verify(exactly = 0) { updateSessionPort.update(any()) }
  }

  @Test
  fun `should mark lap as PB and demote previous PB when faster`() {
    val cmd = command(lapTimeMs = 90_000L, lapNumber = 5)
    val createdId = LapId(200L)
    val createdUid = Uid()
    val previousPb =
      Lap(
        id = LapId(150L),
        uid = Uid(),
        sessionId = sessionId,
        sessionUId = sessionUid,
        carId = playerCarId,
        car = Car("Audi R8 LMS Evo"),
        recordedAt = Clock.System.now(),
        lapTime = LapTimeMs(95_000L),
        lapNumber = LapNumber(2),
        valid = ValidLap(true),
        personalBest = PersonalBest(true),
        track = Track("testTrack"),
        playerLap = true,
      )
    val updates = mutableListOf<Lap>()

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = createdId, uid = createdUid)
      }
    every { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) } returns previousPb
    every { updateLapPort.update(capture(updates)) } answers { firstArg() }

    val result = service.createLap(cmd)

    // Old PB was demoted, new lap was promoted — order matters only insofar as both happen.
    assertThat(updates).hasSize(2)
    val demoted = updates.first { it.id == previousPb.id }
    val promoted = updates.first { it.id == createdId }
    assertThat(demoted.personalBest).isEqualTo(PersonalBest(false))
    assertThat(promoted.personalBest).isEqualTo(PersonalBest(true))

    assertThat(result.personalBest).isEqualTo(PersonalBest(true))
  }

  @Test
  fun `should not promote lap when slower than current PB`() {
    val cmd = command(lapTimeMs = 99_000L, lapNumber = 5)
    val previousPb =
      Lap(
        id = LapId(150L),
        uid = Uid(),
        sessionId = sessionId,
        sessionUId = sessionUid,
        carId = playerCarId,
        car = Car("Audi R8 LMS Evo"),
        recordedAt = Clock.System.now(),
        lapTime = LapTimeMs(95_000L),
        lapNumber = LapNumber(2),
        valid = ValidLap(true),
        personalBest = PersonalBest(true),
        track = Track("testTrack"),
        playerLap = true,
      )

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = LapId(200L), uid = Uid())
      }
    every { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) } returns previousPb

    val result = service.createLap(cmd)

    verify(exactly = 0) { updateLapPort.update(any()) }
    assertThat(result.personalBest).isEqualTo(PersonalBest(false))
  }

  @Test
  fun `should never promote invalid lap, regardless of lap time`() {
    val cmd = command(lapTimeMs = 80_000L, lapNumber = 5, valid = false)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns session()
    every { createLapPort.create(any()) } answers
      {
        firstArg<Lap>().copy(id = LapId(200L), uid = Uid())
      }

    val result = service.createLap(cmd)

    // Invalid lap never queries for current PB and never updates anything on the lap side.
    verify(exactly = 0) { searchLapPort.searchForOne(any<LapSearchCriteria>(), any()) }
    verify(exactly = 0) { updateLapPort.update(any()) }
    assertThat(result.personalBest).isEqualTo(PersonalBest(false))
  }

  @Test
  fun `should throw NotFoundException when session does not exist`() {
    val cmd = command(lapTimeMs = 90_000L, lapNumber = 1)
    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    assertThatThrownBy { service.createLap(cmd) }.isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `should not emit event when session does not exist`() {
    val cmd = command(lapTimeMs = 90_000L, lapNumber = 1)
    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    runCatching { service.createLap(cmd) }

    verify(exactly = 0) { eventPort.emit(any()) }
  }
}
