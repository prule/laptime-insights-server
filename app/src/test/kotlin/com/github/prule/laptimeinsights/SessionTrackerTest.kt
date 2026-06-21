package com.github.prule.laptimeinsights

import com.github.prule.acc.messages.AccBroadcastingInbound.SessionPhase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SessionTrackerTest {

  private val tracker = SessionTracker()

  @Test
  fun `starts a session on the first session phase`() {
    val decision = tracker.observe(0, "RACE", SessionPhase.SESSION)

    assertThat(decision).isEqualTo(SessionBoundary.Start(SessionIdentity(0, "RACE")))
    assertThat(tracker.activeIdentity()).isEqualTo(SessionIdentity(0, "RACE"))
  }

  @Test
  fun `starts on PRE_SESSION too`() {
    val decision = tracker.observe(0, "RACE", SessionPhase.PRE_SESSION)

    assertThat(decision).isEqualTo(SessionBoundary.Start(SessionIdentity(0, "RACE")))
  }

  @Test
  fun `any non-terminal phase before any session starts one`() {
    // Practice/qualifying are often joined mid-stream at a non-start phase; opening on any
    // non-terminal phase ensures their laps aren't dropped for lack of an active session.
    val decision = tracker.observe(0, "PRACTICE", SessionPhase.STARTING)

    assertThat(decision).isEqualTo(SessionBoundary.Start(SessionIdentity(0, "PRACTICE")))
    assertThat(tracker.activeIdentity()).isEqualTo(SessionIdentity(0, "PRACTICE"))
  }

  @Test
  fun `joining a qualifying session mid-stream opens it and attributes following frames`() {
    // First frame seen is FORMATION_LAP (a non-start, non-terminal phase) — still opens.
    assertThat(tracker.observe(1, "QUALIFYING", SessionPhase.FORMATION_LAP))
      .isEqualTo(SessionBoundary.Start(SessionIdentity(1, "QUALIFYING")))
    // Subsequent frames of the same identity keep recording against it (where laps attach).
    assertThat(tracker.observe(1, "QUALIFYING", SessionPhase.SESSION))
      .isEqualTo(SessionBoundary.Continue)
    assertThat(tracker.activeIdentity()).isEqualTo(SessionIdentity(1, "QUALIFYING"))
  }

  @Test
  fun `terminal phase before any session is ignored`() {
    assertThat(tracker.observe(0, "RACE", SessionPhase.SESSION_OVER))
      .isEqualTo(SessionBoundary.Continue)
    assertThat(tracker.activeIdentity()).isNull()
  }

  @Test
  fun `continues while identity and phase are stable`() {
    tracker.observe(0, "RACE", SessionPhase.SESSION)

    assertThat(tracker.observe(0, "RACE", SessionPhase.SESSION)).isEqualTo(SessionBoundary.Continue)
  }

  @Test
  fun `second race with a new session index ends then starts`() {
    tracker.observe(0, "RACE", SessionPhase.SESSION)

    val decision = tracker.observe(1, "RACE", SessionPhase.SESSION)

    assertThat(decision).isEqualTo(SessionBoundary.EndThenStart(SessionIdentity(1, "RACE")))
    assertThat(tracker.activeIdentity()).isEqualTo(SessionIdentity(1, "RACE"))
  }

  @Test
  fun `a session type switch at the same index ends then starts`() {
    tracker.observe(0, "PRACTICE", SessionPhase.SESSION)

    val decision = tracker.observe(0, "RACE", SessionPhase.SESSION)

    assertThat(decision).isEqualTo(SessionBoundary.EndThenStart(SessionIdentity(0, "RACE")))
  }

  @Test
  fun `terminal phase ends the active session`() {
    tracker.observe(0, "RACE", SessionPhase.SESSION)

    val decision = tracker.observe(0, "RACE", SessionPhase.SESSION_OVER)

    assertThat(decision).isEqualTo(SessionBoundary.End)
    assertThat(tracker.activeIdentity()).isNull()
  }

  @Test
  fun `POST_SESSION and RESULT_UI also end the active session`() {
    tracker.observe(0, "RACE", SessionPhase.SESSION)
    assertThat(tracker.observe(0, "RACE", SessionPhase.POST_SESSION)).isEqualTo(SessionBoundary.End)

    tracker.observe(1, "RACE", SessionPhase.SESSION)
    assertThat(tracker.observe(1, "RACE", SessionPhase.RESULT_UI)).isEqualTo(SessionBoundary.End)
  }

  @Test
  fun `a new session starts after a finalized one`() {
    tracker.observe(0, "RACE", SessionPhase.SESSION)
    tracker.observe(0, "RACE", SessionPhase.SESSION_OVER)

    val decision = tracker.observe(1, "RACE", SessionPhase.PRE_SESSION)

    assertThat(decision).isEqualTo(SessionBoundary.Start(SessionIdentity(1, "RACE")))
  }

  @Test
  fun `full two-race weekend on one connection produces two sessions`() {
    // Race 1
    assertThat(tracker.observe(0, "RACE", SessionPhase.PRE_SESSION))
      .isEqualTo(SessionBoundary.Start(SessionIdentity(0, "RACE")))
    assertThat(tracker.observe(0, "RACE", SessionPhase.SESSION)).isEqualTo(SessionBoundary.Continue)
    assertThat(tracker.observe(0, "RACE", SessionPhase.SESSION_OVER)).isEqualTo(SessionBoundary.End)
    // Race 2
    assertThat(tracker.observe(1, "RACE", SessionPhase.PRE_SESSION))
      .isEqualTo(SessionBoundary.Start(SessionIdentity(1, "RACE")))
    assertThat(tracker.observe(1, "RACE", SessionPhase.SESSION_OVER)).isEqualTo(SessionBoundary.End)
  }
}
