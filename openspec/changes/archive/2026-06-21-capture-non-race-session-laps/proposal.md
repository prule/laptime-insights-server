## Why

Practice and qualifying laps are missing from recorded sessions — only race laps appear. The live ingestion layer opens a session **only** when it observes a `RealtimeUpdate` whose phase is `PRE_SESSION` or `SESSION` while no session is active, and lap recording is gated on a session being open (`buildLapCompleted` requires `session != null`). Races reliably pass through `PRE_SESSION`, so a session is open before the first race lap. Practice and qualifying are "free" sessions that the broadcast client typically joins mid-stream, or whose first observed frames carry a non-start phase (`NONE`/`STARTING`/`RESULT_UI` lingering from the previous session); any `LAPCOMPLETED` arriving before a start phase is seen is silently dropped. Result: non-race laps are lost.

## What Changes

- Open a session lazily for **any** non-terminal `RealtimeUpdate` when none is active, instead of requiring a `PRE_SESSION`/`SESSION` start phase. Joining a practice or qualifying session mid-stream now opens a session immediately so its laps are captured.
- Keep terminal phases (`SESSION_OVER`/`POST_SESSION`/`RESULT_UI`) as non-opening when no session is active — the system still does not create a session merely to observe a finished session's result screen.
- Add diagnostic logging of `(sessionIndex, sessionType, phase)` on each boundary decision to confirm phase sequences against real telemetry.
- Update `SessionTracker` unit tests, docs (`real-time-updates.md`), and the `session-boundary-detection` spec to reflect the broadened start rule.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `session-boundary-detection`: the "Start a session on first session phase" requirement broadens — a session starts on any non-terminal phase when none is active, not only `PRE_SESSION`/`SESSION`. This is what causes practice/qualifying laps to be captured.

## Impact

- Code: `app/.../SessionTracker.kt` (`observe` start-branch, `isStartPhase` usage), `ClientInitializer.kt` (logging only). No persistence or API schema change.
- Tests: `SessionTrackerTest.kt` (new/changed start-phase scenarios).
- Docs: `docs/real-time-updates.md` session-segmentation section.
- Behavioral: more sessions may be created at the very edges of a connection (e.g. opening during `FORMATION_LAP`); identity key `(sessionIndex, sessionType)` keeps them correctly attributed. No breaking change to consumers.
