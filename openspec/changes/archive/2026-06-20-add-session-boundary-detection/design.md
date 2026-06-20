## Context

Live ACC ingestion runs in `app/.../ClientInitializer.kt`. It holds a single mutable `session: Session?` field. On the first `RealtimeUpdate` whose `phase()` is `PRE_SESSION` or `SESSION` it creates and starts a session, then leaves it non-null for the lifetime of the connection. Every other listener (`buildRealTimeCarUpdate`, `buildLapCompleted`) is gated on `session != null`, so once a session exists all subsequent laps are attributed to it. There is a `StartSession` use case but no end/finalize counterpart, and `Session` has no end timestamp (`Session.kt` documents lifetime as "bounded by `startedAt` only").

The ACC `RealtimeUpdate` message exposes `sessionIndex(): Int`, `sessionType(): SessionType`, and `phase(): SessionPhase` (enum `NONE, STARTING, PRE_FORMATION, FORMATION_LAP, PRE_SESSION, SESSION, SESSION_OVER, POST_SESSION, RESULT_UI`). These are sufficient to segment the stream into distinct sessions.

## Goals / Non-Goals

**Goals:**
- Segment a single live connection into multiple sessions so consecutive races are stored separately.
- Make the boundary decision logic unit-testable in isolation from the ACC client.
- Give sessions an explicit, persisted end time via an `EndSession` use case.

**Non-Goals:**
- Re-splitting sessions that were already merged in historical data (no backfill/migration of existing rows beyond adding the column).
- Changing how laps, driving time, or car/track resolution work within a session.
- Handling a full ACC restart mid-connection beyond what the identity key already covers.

## Decisions

### Decision: Composite identity key `(sessionIndex, sessionType)` is the primary boundary signal
A `RealtimeUpdate` carries `sessionIndex` and `sessionType`. The active session remembers the key it was started with; any update whose key differs means ACC has moved to a different session, so the active one is finalized and a new one started. This is robust even if terminal-phase updates are missed (dropped packets, fast menu transitions).

- **Alternative — phase transitions only**: relies on observing `SESSION_OVER`/`POST_SESSION`; brittle if those frames are missed. Used as a *secondary* signal instead.
- **Alternative — `sessionIndex` alone**: misses a type switch at the same index; including `sessionType` is cheap insurance.

### Decision: Terminal phases finalize the active session
Phases `SESSION_OVER`, `POST_SESSION`, `RESULT_UI` finalize the active session and record its end time, clearing the active session. This yields an accurate end timestamp at the natural moment rather than waiting for the next session's first frame. After finalizing, the existing "start on `PRE_SESSION`/`SESSION`" path creates the next session.

### Decision: Extract a `SessionTracker` collaborator
Move the start/end/identity decision out of `ClientInitializer` into a small `SessionTracker` that, given an observed `(sessionIndex, sessionType, phase)`, returns a boundary decision (`Start`, `Continue`, `EndThenStart`, `End`). `ClientInitializer` executes the decision (calling create/start/end use cases and resetting `SessionState`). This mirrors the existing testable `SessionState` and keeps the ACC wiring thin. Decision: pure function/class with no I/O so it is trivially unit-testable.

- **Alternative — inline in `ClientInitializer`**: faster but untestable without a live client; the class is already heavy.

### Decision: `EndSession` use case + `endedAt` on `Session`
Add `endedAt: Instant?` to `Session` with an `end(time)` method (idempotent: ending an already-ended session does not overwrite/corrupt the recorded end). Add `EndSessionUseCase`, `EndSessionCommand`, `EndSessionService`, persisted through `SessionPersistenceAdapter`. Requires an Exposed column and a Flyway-style migration (`db/migrations/`) adding a nullable `ended_at` column.

### Decision: Reset `SessionState` on every new session
`SessionState` (lap counts, per-car validity, car models) is per-session. On `Start`/`EndThenStart`, `ClientInitializer` replaces it with a fresh instance so lap numbering and validity restart cleanly.

## Risks / Trade-offs

- **Missed terminal phase** → identity-key change still triggers `EndThenStart`, so correctness does not depend on seeing the terminal frame.
- **`sessionIndex` repeats after a full game restart on a surviving connection** → could mistake a new session for the same one. Mitigation: in practice the broadcast connection drops on restart (new `ClientInitializer`/`session`), so the surviving-connection-with-reset-index case is out of scope (noted in Non-Goals).
- **Idempotent end vs. real second race at same index** → mitigated by the composite key and by treating identity change as authoritative over phase.
- **New `endedAt` column** → existing rows get `NULL`; readers must treat null end as "unknown/legacy", consistent with current "no end timestamp" behavior.
