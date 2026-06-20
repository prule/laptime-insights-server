package com.github.prule.laptimeinsights.application.domain.model

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SessionTest {

  private fun newSession(startedAt: kotlin.time.Instant? = null) =
    Session(
      id = SessionId(1L),
      uid = Uid(),
      startedAt = startedAt,
      simulator = Simulator.ACC,
      track = Track("Monza"),
      car = Car("Ferrari 488 GT3"),
      sessionType = SessionType("Race"),
    )

  @Test
  fun `session can be started if not already started`() {
    val session = newSession()
    val startTime = Clock.System.now()

    session.start(startTime)

    assertThat(session.isStarted()).isTrue()
    assertThat(session.startedAt()).isEqualTo(startTime)
  }

  @Test
  fun `session cannot be started if already started`() {
    val startTime = Clock.System.now()
    val session = newSession(startedAt = startTime)
    val newStartTime = Clock.System.now().plus(60.seconds)

    assertThatThrownBy { session.start(newStartTime) }
      .isInstanceOf(IllegalStateException::class.java)
      .hasMessage("Session cannot be started")

    // Original start time preserved.
    assertThat(session.isStarted()).isTrue()
    assertThat(session.startedAt()).isEqualTo(startTime)
  }

  @Test
  fun `session can be ended`() {
    val session = newSession(startedAt = Clock.System.now())
    val endTime = Clock.System.now().plus(300.seconds)

    session.end(endTime)

    assertThat(session.isEnded()).isTrue()
    assertThat(session.endedAt()).isEqualTo(endTime)
  }

  @Test
  fun `ending an already-ended session preserves the original end time`() {
    val session = newSession(startedAt = Clock.System.now())
    val endTime = Clock.System.now().plus(300.seconds)
    session.end(endTime)

    session.end(endTime.plus(60.seconds))

    // First end wins — a later finalize does not overwrite the recorded end.
    assertThat(session.endedAt()).isEqualTo(endTime)
  }

  @Test
  fun `a fresh session is not ended`() {
    assertThat(newSession().isEnded()).isFalse()
    assertThat(newSession().endedAt()).isNull()
  }

  @Test
  fun `drivingTime defaults to zero on a fresh session`() {
    assertThat(newSession().drivingTime()).isEqualTo(LapTimeMs(0))
  }

  @Test
  fun `addDriving accumulates lap times into drivingTime`() {
    val session = newSession()

    session.addDriving(LapTimeMs(95_000L))
    session.addDriving(LapTimeMs(91_500L))
    session.addDriving(LapTimeMs(94_200L))

    // Sum independent of order/validity — every call adds, the caller decides which laps to feed.
    assertThat(session.drivingTime()).isEqualTo(LapTimeMs(95_000L + 91_500L + 94_200L))
  }
}
