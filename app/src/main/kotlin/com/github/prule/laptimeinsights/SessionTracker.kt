package com.github.prule.laptimeinsights

import com.github.prule.acc.messages.AccBroadcastingInbound.SessionPhase

/**
 * Identity of an ACC session as seen on the live `RealtimeUpdate` stream. ACC increments
 * [sessionIndex] when it moves to a different session within a connection; [sessionType] guards the
 * rare case of a type switch at the same index. Together they key the active session so consecutive
 * races on one connection are not merged.
 */
data class SessionIdentity(val sessionIndex: Int, val sessionType: String)

/**
 * Decision returned by [SessionTracker.observe] for the ingestion layer to execute.
 *
 * The tracker only decides boundaries; [ClientInitializer] performs the create/start/end use-case
 * calls and resets per-session state.
 */
sealed interface SessionBoundary {
  /** No boundary — keep recording against the current session (or keep waiting for one). */
  data object Continue : SessionBoundary

  /** Start a new session for [identity]; no session was active. */
  data class Start(val identity: SessionIdentity) : SessionBoundary

  /** Finalize the active session for [identity]; nothing active afterward. */
  data object End : SessionBoundary

  /** Finalize the active session, then start a new one for [identity] (identity changed). */
  data class EndThenStart(val identity: SessionIdentity) : SessionBoundary
}

/**
 * Segments the live ACC telemetry stream into discrete sessions (Option C from the
 * `add-session-boundary-detection` design):
 * - **identity change** (`sessionIndex`/`sessionType` differ from the active key) is the primary,
 *   authoritative "new session" signal — robust even if terminal-phase frames are missed;
 * - **terminal phase** ([SessionPhase.SESSION_OVER], [SessionPhase.POST_SESSION],
 *   [SessionPhase.RESULT_UI]) finalizes the active session at the natural moment so its end time is
 *   accurate.
 *
 * Pure and free of I/O: state is only the active [SessionIdentity], updated as decisions are issued
 * so the class is trivially unit-testable. Not thread-safe; the ACC client delivers updates on a
 * single listener thread.
 */
class SessionTracker {
  private var active: SessionIdentity? = null

  /** The identity of the currently active session, or null if none is active. */
  fun activeIdentity(): SessionIdentity? = active

  fun observe(sessionIndex: Int, sessionType: String, phase: SessionPhase): SessionBoundary {
    val observed = SessionIdentity(sessionIndex, sessionType)
    val current = active

    if (current == null) {
      // No session yet — any non-terminal update opens one. A bare start phase
      // (PRE_SESSION/SESSION) is no longer required: practice and qualifying are often joined
      // mid-stream, or their first observed frame carries a non-start phase, so gating on a start
      // phase silently dropped their laps. Terminal phases still never open a session — we don't
      // create one just to observe a finished session's result screen.
      return if (phase.isTerminalPhase()) {
        SessionBoundary.Continue
      } else {
        active = observed
        SessionBoundary.Start(observed)
      }
    }

    // A session is active. An identity change on any non-terminal phase rolls over to the new
    // session; an identity change that arrives on a terminal phase falls through to End below and
    // the new identity opens lazily on its next non-terminal frame.
    if (observed != current && !phase.isTerminalPhase()) {
      active = observed
      return SessionBoundary.EndThenStart(observed)
    }

    if (phase.isTerminalPhase()) {
      active = null
      return SessionBoundary.End
    }

    return SessionBoundary.Continue
  }

  private fun SessionPhase.isTerminalPhase() =
    this == SessionPhase.SESSION_OVER ||
      this == SessionPhase.POST_SESSION ||
      this == SessionPhase.RESULT_UI
}
