package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionStarted
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.StartSessionCommand
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import com.github.prule.laptimeinsights.tracker.utils.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class StartSessionServiceTest {

  private val updateSessionPort = mockk<UpdateSessionPort>()
  private val searchSessionPort = mockk<SearchSessionPort>()
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service = StartSessionService(updateSessionPort, searchSessionPort, eventPort)

  @Test
  fun `should start session successfully`() {
    val uid = Uid()
    val startTime = Clock.System.now()
    val existingSession =
      Session(
        id = SessionId(1L),
        uid = uid,
        startedAt = null,
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Spa"),
        car = Car("Ferrari"),
        sessionType = SessionType("Qualifying"),
      )
    val command = StartSessionCommand(uid, startTime)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0
    every { eventPort.emit(any()) } returns Unit

    val result = service.startSession(command)

    assertThat(result.isStarted()).isTrue()
    assertThat(result.startedAt()).isEqualTo(startTime)
    verify { updateSessionPort.update(match { it.startedAt() == startTime }) }
    verify { eventPort.emit(match { it is SessionStarted && it.session.uid == uid }) }
  }

  @Test
  fun `should not emit event if session does not exist`() {
    val uid = Uid()
    val command = StartSessionCommand(uid, Clock.System.now())

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    runCatching { service.startSession(command) }

    verify(exactly = 0) { eventPort.emit(any()) }
  }

  @Test
  fun `should throw NotFoundException if session does not exist`() {
    val uid = Uid()
    val command = StartSessionCommand(uid, Clock.System.now())

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    assertThatThrownBy { service.startSession(command) }.isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `should throw IllegalStateException if session is already started`() {
    val uid = Uid()
    val startTime = Clock.System.now()
    val existingSession =
      Session(
        id = SessionId(1L),
        uid = uid,
        startedAt = startTime,
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Spa"),
        car = Car("Ferrari"),
        sessionType = SessionType("Qualifying"),
      )
    val command = StartSessionCommand(uid, startTime.plus(10.seconds))

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession

    assertThatThrownBy { service.startSession(command) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Session cannot be started")
  }
}
