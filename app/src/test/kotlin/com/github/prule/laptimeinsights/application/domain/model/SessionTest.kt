package com.github.prule.laptimeinsights.application.domain.model

import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SessionTest {

  @Test
  fun `session can be started if not already started`() {
    val session =
      Session(
        id = SessionId(1L),
        uid = Uid(),
        startedAt = null,
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Monza"),
        car = Car("Ferrari 488 GT3"),
        sessionType = SessionType("Race"),
      )
    val startTime = Clock.System.now()

    session.start(startTime)

    assertThat(session.isStarted()).isTrue()
    assertThat(session.startedAt()).isEqualTo(startTime)
  }

  @Test
  fun `session cannot be started if already started`() {
    val startTime = Clock.System.now()
    val session =
      Session(
        id = SessionId(1L),
        uid = Uid(),
        startedAt = startTime,
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Monza"),
        car = Car("Ferrari 488 GT3"),
        sessionType = SessionType("Race"),
      )
    val newStartTime = Clock.System.now().plus(60.seconds)

    session.start(newStartTime)

    assertThat(session.isStarted()).isTrue()
    assertThat(session.startedAt()).isEqualTo(startTime) // Should remain the original start time
  }

  @Test
  fun `session can be finished if started and not already finished`() {
    val startTime = Clock.System.now()
    val session =
      Session(
        id = SessionId(1L),
        uid = Uid(),
        startedAt = startTime,
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Monza"),
        car = Car("Ferrari 488 GT3"),
        sessionType = SessionType("Race"),
      )
    val finishTime = Clock.System.now().plus(3600.seconds)

    session.finish(finishTime)

    assertThat(session.isFinished()).isTrue()
    assertThat(session.finishedAt()).isEqualTo(finishTime)
  }

  @Test
  fun `session cannot be finished if not started`() {
    val session =
      Session(
        id = SessionId(1L),
        uid = Uid(),
        startedAt = null,
        finishedAt = null,
        simulator = Simulator.ACC,
        track = Track("Monza"),
        car = Car("Ferrari 488 GT3"),
        sessionType = SessionType("Race"),
      )
    val finishTime = Clock.System.now().plus(3600.seconds)

    session.finish(finishTime)

    assertThat(session.isFinished()).isFalse()
    assertThat(session.finishedAt()).isNull()
  }

  @Test
  fun `session cannot be finished if already finished`() {
    val startTime = Clock.System.now()
    val firstFinishTime = Clock.System.now().plus(3600.seconds)
    val session =
      Session(
        id = SessionId(1L),
        uid = Uid(),
        startedAt = startTime,
        finishedAt = firstFinishTime,
        simulator = Simulator.ACC,
        track = Track("Monza"),
        car = Car("Ferrari 488 GT3"),
        sessionType = SessionType("Race"),
      )
    val secondFinishTime = Clock.System.now().plus(7200.seconds)

    session.finish(secondFinishTime)

    assertThat(session.isFinished()).isTrue()
    assertThat(session.finishedAt())
      .isEqualTo(firstFinishTime) // Should remain the original finish time
  }
}
