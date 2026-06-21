## Context

`SessionTracker.observe()` decides session boundaries on the live ACC `RealtimeUpdate` stream; `ClientInitializer` executes the create/start/end use cases and gates lap recording on `session != null` (`buildLapCompleted`). Today, when no session is active, `observe()` only opens one if the phase is a *start phase* (`PRE_SESSION` or `SESSION`); every other phase returns `Continue` and leaves `active == null`.

Race sessions reliably emit `PRE_SESSION` before green, so a session is open before the first race lap. Practice and qualifying are free, looping sessions that the broadcast client commonly joins mid-stream, and the gap between sessions on one connection can leave lingering non-start phases (`RESULT_UI`, `STARTING`, `NONE`). A `LAPCOMPLETED` that arrives while `active == null` is dropped — which is why only race laps appear.

Constraint: `SessionTracker` is pure and unit-tested; the fix must stay I/O-free and keep the composite identity key `(sessionIndex, sessionType)` authoritative for attribution.

## Goals / Non-Goals

**Goals:**
- Capture practice and qualifying laps by ensuring a session is open whenever live telemetry is flowing.
- Keep correct per-session attribution and end detection unchanged.
- Add telemetry logging to confirm phase sequences against real ACC data.

**Non-Goals:**
- Backfilling laps already lost in prior recordings.
- Filtering or special-casing by `sessionType` anywhere in the lap pipeline (it is, and stays, type-agnostic).
- Changing persistence, REST/HATEOAS, or frontend schemas.

## Decisions

**Decision: Open a session on any non-terminal phase when none is active.**
Change the `current == null` branch of `observe()` from "start only on `isStartPhase()`" to "start on any phase that is *not* terminal". Terminal phases (`SESSION_OVER`/`POST_SESSION`/`RESULT_UI`) still return `Continue` when nothing is active, so the system never opens a session merely to watch a finished session's result screen.

- *Why over alternatives:*
  - *Open also on LAPCOMPLETED:* a lap can only complete inside an active session, so opening on the first non-terminal `RealtimeUpdate` (which always precedes laps) covers it without threading session-open logic into two listeners.
  - *Enumerate more "running" phases (add `STARTING`, `FORMATION_LAP`, …):* brittle — depends on exhaustively guessing ACC's phase set. Inverting to "anything non-terminal" is robust to phases we haven't catalogued.
  - *Keep start-phase-only and live with the loss:* rejected — it is the bug.

**Decision: Identity attribution is unchanged.**
The opened session still records `(sessionIndex, sessionType)`; a later identity change still triggers `EndThenStart`. Opening "early" (e.g. during `FORMATION_LAP`) is harmless because the identity is already correct at that point.

**Decision: Log every boundary decision.**
Emit `(sessionIndex, sessionType, phase) -> decision` at info/debug so a captured weekend can confirm practice/qualifying phase sequences and prove the fix.

## Risks / Trade-offs

- [Spurious early session during pre-race phases (`FORMATION_LAP`)] → Acceptable: identity is already correct, and the session would be created moments later anyway; no duplicate because identity is stable.
- [Opening a session from a stray mid-stream frame with an as-yet-unknown `sessionType` → `"Unknown"`] → Same behavior as today's start-phase path; `sessionType` is corrected by subsequent updates / identity key. No regression.
- [More sessions created at connection edges] → Bounded by identity key; terminal-only frames still never open a session.

## Migration Plan

Pure logic change in `SessionTracker` plus added logging; no schema or data migration. Deploy by normal release. Rollback = revert the commit. Verify against a captured practice→qualifying→race weekend that all three session types now have laps.

## Open Questions

- Confirm with real telemetry which non-start phases practice/qualifying actually present at join time (logging added here answers this). If ACC turns out to always emit `SESSION` for running free sessions, the residual gap is solely the inter-session lingering-phase window — the fix covers both cases regardless.
