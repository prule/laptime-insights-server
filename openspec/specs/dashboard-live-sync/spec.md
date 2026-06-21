# dashboard-live-sync

## Purpose

Keep the dashboard's views up to date in LIVE data mode by driving cache
invalidation from a single shared live-event connection, so mounted screens
refresh automatically as laps and sessions change in the sim.

## Requirements

### Requirement: Shared live event connection

The frontend SHALL maintain a single application-level WebSocket connection to the
index `live` rel while in LIVE data mode, and SHALL expose its events to all screens
through a shared subscription rather than per-screen sockets.

#### Scenario: Single connection in live mode

- **WHEN** the app is running in LIVE data mode and the index advertises a `live` rel
- **THEN** exactly one WebSocket connection to that rel is open regardless of how many
  screens are mounted

#### Scenario: No connection when unavailable

- **WHEN** the app is in MOCK data mode, or the index does not advertise a `live` rel
- **THEN** no WebSocket connection is opened and no cache invalidation occurs

#### Scenario: Automatic reconnect

- **WHEN** the shared WebSocket closes unexpectedly while still in LIVE mode
- **THEN** the connection is retried automatically and event-driven sync resumes once
  re-established

### Requirement: Lap events refresh lap-derived views

The frontend SHALL invalidate the TanStack Query caches that depend on lap data when a
`LapCreated` event is received, so any mounted screen showing that data refetches and
re-renders without manual refresh.

#### Scenario: Completed lap updates the dashboard

- **WHEN** a `LapCreated` event arrives over the shared connection
- **THEN** the laps list, lap aggregate, session-laps, and session/track best-lap
  queries are invalidated and refetched while their consuming screens are mounted

#### Scenario: Lap appears on the laps screen without navigation

- **WHEN** the user is viewing the Laps screen and a new lap is completed in the sim
- **THEN** the new lap appears in the list without the user refreshing or navigating away

### Requirement: Session events refresh session-derived views

The frontend SHALL invalidate the TanStack Query caches that depend on session data when
a `SessionCreated`, `SessionStarted`, `SessionUpdated`, or `SessionEnded` event is
received.

#### Scenario: Session lifecycle updates session views

- **WHEN** any session lifecycle event arrives over the shared connection
- **THEN** the sessions list and session aggregate queries are invalidated, and the
  affected `session` record query is invalidated so its mounted screen reflects the new
  state (including a populated `endedAt` on `SessionEnded`)

### Requirement: Live screen reuses the shared connection

`LiveScreen` SHALL consume the shared live-event subscription instead of opening its own
WebSocket, preserving its existing telemetry, lap, and track-map behavior.

#### Scenario: No duplicate socket from the live screen

- **WHEN** the user navigates to the Live screen in LIVE mode
- **THEN** no second WebSocket connection is opened; the live screen renders from the
  shared connection's events
