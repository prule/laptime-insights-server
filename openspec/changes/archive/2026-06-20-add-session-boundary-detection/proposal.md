## Why

When the ACC broadcasting connection stays alive across two back-to-back races, all laps are recorded into a single session. The ingestion code creates a session on the first session-phase update and never closes it, so a "2 races of 5 laps" weekend is stored as one 10-lap session. Sessions are the primary unit of analysis in the dashboard, so merged sessions corrupt every downstream insight (pace, consistency, comparisons).

## What Changes

- Detect ACC session boundaries from the live `RealtimeUpdate` stream instead of starting exactly one session per connection.
- Identify the active ACC session by a composite key of `sessionIndex` + `sessionType`. When the observed key differs from the active session's key, finalize the old session and start a new one.
- Treat terminal session phases (`SESSION_OVER`, `POST_SESSION`, `RESULT_UI`) as the trigger to finalize the active session and record its end time, after which the next `PRE_SESSION`/`SESSION` update starts a fresh session.
- Add an **EndSession** use case (port, command, service) so a session has an explicit, persisted end — today sessions can only start.
- Extract session-boundary decision logic out of `ClientInitializer` into a small, unit-testable `SessionTracker` collaborator.

## Capabilities

### New Capabilities
- `session-boundary-detection`: How the live ACC telemetry stream is segmented into discrete sessions — when a new session starts, when the active session ends, and the signals (session identity key + phase transitions) that drive those decisions.

### Modified Capabilities
<!-- None: there is no existing session-ingestion spec; boundary detection is captured as a new capability. -->

## Impact

- `app` ingestion: `ClientInitializer` (session start/end decisions), new `SessionTracker`, `SessionState` reset on boundary.
- Domain/application: new `EndSession` use case (`application/port/in/session`, `application/domain/service/session`); `Session` model gains an end/finalize step.
- Persistence: session end time persisted (`SessionPersistenceAdapter`, possibly a migration if no end-time column exists).
- Tests: new `SessionTracker` unit tests; `EndSessionService` test; existing session/ingestion tests updated.
- Docs: `docs/real-time-updates.md` and `docs/user-guide.md` updated to describe session segmentation.
