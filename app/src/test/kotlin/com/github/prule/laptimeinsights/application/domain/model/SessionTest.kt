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
