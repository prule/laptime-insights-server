package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.SessionUpdated
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.UpdateSessionCommand
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Clock
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class UpdateSessionServiceTest {

  private val updateSessionPort = mockk<UpdateSessionPort>()
  private val searchSessionPort = mockk<SearchSessionPort>()
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service = UpdateSessionService(updateSessionPort, searchSessionPort, eventPort)

  @Test
  fun `should update session and emit SessionUpdated event`() {
    val uid = Uid()
    val existingSession =
      Session(
        id = SessionId(1L),
        uid = uid,
        startedAt = Clock.System.now(),
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Spa"),
        car = Car("Ferrari"),
        sessionType = SessionType("Qualifying"),
      )
    val newTrack = Track("Monza")
    val newCar = Car("Porsche 911 GT3 R")
    val command = UpdateSessionCommand(uid, newTrack, newCar)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0
    every { eventPort.emit(any()) } returns Unit

    val result = service.update(command)

    assertThat(result.track).isEqualTo(newTrack)
    assertThat(result.car).isEqualTo(newCar)
    verify { updateSessionPort.update(match { it.track == newTrack && it.car == newCar }) }
    verify { eventPort.emit(match { it is SessionUpdated && it.session.uid == uid }) }
  }

  @Test
  fun `should keep existing track when command supplies the same track value`() {
    val uid = Uid()
    val track = Track("Spa")
    val existingSession =
      Session(
        id = SessionId(1L),
        uid = uid,
        startedAt = Clock.System.now(),
        finishedAt = null,
        simulator = Simulator.ACC,
        track = track,
        car = Car("Ferrari"),
        sessionType = SessionType("Qualifying"),
      )
    val command = UpdateSessionCommand(uid, track = track, car = Car("Ferrari"))

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0
    every { eventPort.emit(any()) } returns Unit

    val result = service.update(command)

    assertThat(result.track).isEqualTo(track)
    assertThat(result.car).isEqualTo(Car("Ferrari"))
  }

  @Test
  fun `should leave existing field unchanged when command supplies null`() {
    val uid = Uid()
    val existingTrack = Track("Spa")
    val existingCar = Car("Ferrari")
    val existingSession =
      Session(
        id = SessionId(1L),
        uid = uid,
        startedAt = Clock.System.now(),
        finishedAt = null,
        simulator = Simulator.ACC,
        track = existingTrack,
        car = existingCar,
        sessionType = SessionType("Qualifying"),
      )
    // Caller (e.g. ClientInitializer) may pass null when telemetry hasn't been seen yet.
    // That MUST NOT clobber what's already persisted.
    val command = UpdateSessionCommand(uid, track = null, car = null)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0
    every { eventPort.emit(any()) } returns Unit

    val result = service.update(command)

    assertThat(result.track).isEqualTo(existingTrack)
    assertThat(result.car).isEqualTo(existingCar)
  }

  @Test
  fun `should throw NotFoundException if session does not exist`() {
    val uid = Uid()
    val command = UpdateSessionCommand(uid, Track("Monza"), Car("Ferrari"))

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    assertThatThrownBy { service.update(command) }
      .isInstanceOf(NotFoundException::class.java)
      .hasMessage(uid.toString())
  }

  @Test
  fun `should not emit event if session does not exist`() {
    val uid = Uid()
    val command = UpdateSessionCommand(uid, Track("Monza"), Car("Ferrari"))

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    runCatching { service.update(command) }

    verify(exactly = 0) { eventPort.emit(any()) }
  }
}
