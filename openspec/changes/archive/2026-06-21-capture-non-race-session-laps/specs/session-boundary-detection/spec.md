## MODIFIED Requirements

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
