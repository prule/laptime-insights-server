package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionFinished
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.FinishSessionCommand
import com.github.prule.laptimeinsights.application.port.out.EventPort
import com.github.prule.laptimeinsights.application.port.out.session.SearchSessionPort
import com.github.prule.laptimeinsights.application.port.out.session.UpdateSessionPort
import io.ktor.server.plugins.NotFoundException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class FinishSessionServiceTest {

  private val updateSessionPort = mockk<UpdateSessionPort>()
  private val searchSessionPort = mockk<SearchSessionPort>()
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service = FinishSessionService(updateSessionPort, searchSessionPort, eventPort)

  @Test
  fun `should finish session successfully`() {
    val uid = Uid()
    val startTime = Clock.System.now()
    val finishTime = startTime.plus(3600.seconds)
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
    val command = FinishSessionCommand(uid, finishTime)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0
    coEvery { eventPort.emit(any()) } returns Unit

    val result = service.finishSession(command)

    assertThat(result.isFinished()).isTrue()
    assertThat(result.finishedAt()).isEqualTo(finishTime)
    verify { updateSessionPort.update(match { it.finishedAt() == finishTime }) }
    coVerify { eventPort.emit(match { it is SessionFinished && it.session.uid == uid }) }
  }

  @Test
  fun `should throw NotFoundException if session does not exist`() {
    val uid = Uid()
    val command = FinishSessionCommand(uid, Clock.System.now())

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    assertThatThrownBy { service.finishSession(command) }
      .isInstanceOf(NotFoundException::class.java)
  }

  @Test
  fun `should throw IllegalStateException if session is not started`() {
    val uid = Uid()
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
    val command = FinishSessionCommand(uid, Clock.System.now())

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession

    assertThatThrownBy { service.finishSession(command) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Session cannot be finished")
  }

  @Test
  fun `should throw IllegalStateException if session is already finished`() {
    val uid = Uid()
    val startTime = Clock.System.now()
    val finishTime = startTime.plus(3600.seconds)
    val existingSession =
      Session(
        id = SessionId(1L),
        uid = uid,
        startedAt = startTime,
        finishedAt = finishTime,
        simulator = Simulator.ACC,
        track = Track("Spa"),
        car = Car("Ferrari"),
        sessionType = SessionType("Qualifying"),
      )
    val command = FinishSessionCommand(uid, finishTime.plus(60.seconds))

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession

    assertThatThrownBy { service.finishSession(command) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Session cannot be finished")
  }
}
