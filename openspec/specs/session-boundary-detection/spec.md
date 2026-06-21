# session-boundary-detection

## Purpose

Segment the live ACC telemetry (`RealtimeUpdate`) stream into discrete sessions so consecutive sessions on a single broadcasting connection (e.g. two back-to-back races) are recorded separately rather than merged into one. Boundaries are driven by a composite session identity key plus session-phase transitions, and sessions gain an explicit, persisted end.

## Requirements

### Requirement: Active session identity
The ingestion layer SHALL identify the active ACC session by a composite key derived from the live `RealtimeUpdate` stream: `sessionIndex` together with `sessionType`. The active key SHALL be retained for as long as the session is open.

#### Scenario: Identity established on session start
- **WHEN** a session is started from a `RealtimeUpdate`
- **THEN** the system records that update's `sessionIndex` and `sessionType` as the active session's identity key

### Requirement: Start a session on first session phase
The system SHALL start a new session when no session is active and a `RealtimeUpdate` arrives whose phase is **not** a terminal phase (`SESSION_OVER`, `POST_SESSION`, `RESULT_UI`). A terminal phase received while no session is active SHALL NOT start a session. This ensures laps for practice and qualifying sessions — which the broadcast client may join mid-stream, or whose first observed frames carry a non-`PRE_SESSION`/`SESSION` phase — are captured rather than dropped for lack of an open session.

#### Scenario: First session of a connection
- **WHEN** no session is active
- **AND** a `RealtimeUpdate` arrives with phase `PRE_SESSION` or `SESSION`
- **THEN** the system creates and starts a session
- **AND** records the update's identity key as active

#### Scenario: Joining a practice or qualifying session mid-stream
- **WHEN** no session is active
- **AND** a `RealtimeUpdate` arrives with a non-terminal, non-start phase (e.g. `STARTING`, `FORMATION_LAP`)
- **THEN** the system creates and starts a session
- **AND** records the update's identity key as active
- **AND** subsequent laps are recorded against that session

#### Scenario: Terminal phase before any session does not start
- **WHEN** no session is active
- **AND** a `RealtimeUpdate` arrives with a terminal phase (`SESSION_OVER`, `POST_SESSION`, or `RESULT_UI`)
- **THEN** the system does not start a session

### Requirement: Start a new session when identity changes
When a session is active and a `RealtimeUpdate` arrives whose identity key differs from the active key, the system SHALL finalize the active session and start a new session for the new identity. Laps and realtime car updates following the change SHALL be attributed to the new session.

#### Scenario: Second race after first race
- **WHEN** a session is active for a given identity key
- **AND** a `RealtimeUpdate` arrives with a different `sessionIndex` or `sessionType`
- **THEN** the system finalizes the active session
- **AND** starts a new session recorded with the new identity key

#### Scenario: Laps attributed to the correct session
- **WHEN** a new session has started after an identity change
- **AND** a lap completes
- **THEN** the lap is recorded against the new session, not the previous one

### Requirement: Finalize a session on terminal phase
When a session is active and a `RealtimeUpdate` arrives with a terminal phase (`SESSION_OVER`, `POST_SESSION`, or `RESULT_UI`), the system SHALL finalize the active session and record its end time, after which no session is active until a new one starts.

#### Scenario: Session ends with terminal phase
- **WHEN** a session is active
- **AND** a `RealtimeUpdate` arrives with phase `SESSION_OVER`, `POST_SESSION`, or `RESULT_UI`
- **THEN** the system finalizes the active session with an end time
- **AND** no session is active afterward

#### Scenario: New session starts after a finalized one
- **WHEN** a session has been finalized via a terminal phase
- **AND** a later `RealtimeUpdate` arrives with phase `PRE_SESSION` or `SESSION`
- **THEN** the system starts a new session

### Requirement: Per-session state is isolated
Per-session mutable state (lap counts, per-car validity, registered car models) SHALL be reset when a new session starts so that counters and validity from a previous session do not leak into the next.

#### Scenario: Lap numbering restarts per session
- **WHEN** a new session starts after a previous session finalized
- **AND** the first lap of the new session completes
- **THEN** the lap is numbered as the first lap of the new session

### Requirement: Session end is persisted
A session SHALL support an explicit end operation that records the session's end time and persists it, exposed through an `EndSession` use case.

#### Scenario: End time recorded
- **WHEN** a session is finalized
- **THEN** the session's end time is set and persisted

#### Scenario: Ending an already-ended session is safe
- **WHEN** a session has already been finalized
- **AND** a finalize is requested again for the same session
- **THEN** the operation does not corrupt the session's recorded end time
