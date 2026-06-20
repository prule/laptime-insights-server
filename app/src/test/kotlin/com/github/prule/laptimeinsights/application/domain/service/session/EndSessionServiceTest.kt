package com.github.prule.laptimeinsights.application.domain.service.session

import com.github.prule.laptimeinsights.application.domain.model.Car
import com.github.prule.laptimeinsights.application.domain.model.Session
import com.github.prule.laptimeinsights.application.domain.model.SessionEnded
import com.github.prule.laptimeinsights.application.domain.model.SessionId
import com.github.prule.laptimeinsights.application.domain.model.SessionSearchCriteria
import com.github.prule.laptimeinsights.application.domain.model.SessionType
import com.github.prule.laptimeinsights.application.domain.model.Simulator
import com.github.prule.laptimeinsights.application.domain.model.Track
import com.github.prule.laptimeinsights.application.domain.model.Uid
import com.github.prule.laptimeinsights.application.port.`in`.session.EndSessionCommand
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

class EndSessionServiceTest {

  private val updateSessionPort = mockk<UpdateSessionPort>()
  private val searchSessionPort = mockk<SearchSessionPort>()
  private val eventPort = mockk<EventPort>(relaxed = true)
  private val service = EndSessionService(updateSessionPort, searchSessionPort, eventPort)

  private fun session(
    uid: Uid,
    startedAt: kotlin.time.Instant?,
    endedAt: kotlin.time.Instant? = null,
  ) =
    Session(
      id = SessionId(1L),
      uid = uid,
      startedAt = startedAt,
      simulator = Simulator.ACC,
      track = Track("Spa"),
      car = Car("Ferrari"),
      sessionType = SessionType("Race"),
      endedAt = endedAt,
    )

  @Test
  fun `should end session and emit event`() {
    val uid = Uid()
    val endTime = Clock.System.now()
    val existingSession = session(uid, startedAt = endTime.minus(300.seconds))
    val command = EndSessionCommand(uid, endTime)

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0

    val result = service.endSession(command)

    assertThat(result.isEnded()).isTrue()
    assertThat(result.endedAt()).isEqualTo(endTime)
    verify { updateSessionPort.update(match { it.endedAt() == endTime }) }
    verify { eventPort.emit(match { it is SessionEnded && it.session.uid == uid }) }
  }

  @Test
  fun `ending an already-ended session preserves the original end time`() {
    val uid = Uid()
    val originalEnd = Clock.System.now()
    val existingSession =
      session(uid, startedAt = originalEnd.minus(300.seconds), endedAt = originalEnd)
    val command = EndSessionCommand(uid, originalEnd.plus(60.seconds))

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns existingSession
    every { updateSessionPort.update(any()) } returnsArgument 0

    val result = service.endSession(command)

    assertThat(result.endedAt()).isEqualTo(originalEnd)
  }

  @Test
  fun `should throw NotFoundException if session does not exist`() {
    val command = EndSessionCommand(Uid(), Clock.System.now())

    every { searchSessionPort.searchForOne(any<SessionSearchCriteria>()) } returns null

    assertThatThrownBy { service.endSession(command) }.isInstanceOf(NotFoundException::class.java)
    verify(exactly = 0) { eventPort.emit(any()) }
  }
}
