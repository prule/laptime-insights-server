# Specification: Effort Tracking & Session Archiving

## Goal

To quantify the driver's commitment by capturing every session and calculating cumulative physical "effort" (distance
and time).

## Functional Requirements

- **Automatic Ingestion:** When the `acc-client` detects a new session state, the server must initialize a `Session`
  record.
- **Persistence:** Save session metadata: Track, Car, Session Type (Practice, Qualy, Race), and Environment (
  Ambient/Track Temp).
- **Distance Calculation:** For every valid lap completed, increment the total session distance based on track constant
  length.

## Data Points (Domain Model)

- `session_id`: UUID
- `start_timestamp`: ISO8601
- `total_distance_km`: Decimal
- `lap_count`: Integer

## Business Rules

- A session with 0 completed laps should be flagged as "Aborted" and excluded from effort heatmaps.
- Distance is only calculated for "Valid" laps to ensure effort reflects clean driving.

## Proposed API Endpoint

`GET /api/v1/stats/effort?range=last-30-days`

## Implementation status

**Partially implemented.** The session and lap persistence pipeline is fully working; effort aggregation is not.

- Sessions are created and persisted automatically when `acc-client` broadcasts a new session state (via
  `CreateSessionService`, `StartSessionService`, `UpdateSessionService`).
- Session metadata stored: track, car, session type, simulator, `startedAt`, `playerCarId`,
  `drivingTimeMs` (cumulative time on track for the player's car). Environment
  fields (`ambientTemp`, `trackTemp`) are **not** yet captured.
- Sessions have no explicit "finished" timestamp — `drivingTimeMs` (folded in by `CreateLapService`
  on every player-car lap) is the canonical proxy for how much driving the session contained.
- Laps are persisted with `valid` and `personalBest` flags. The `lapCount` per session is derivable
  by querying `GET /api/1/sessions/{uid}/laps`.
- `total_distance_km` is **not** stored. Distance is not calculated; track-length constants are not
  yet defined.
- The "Aborted" session flag is **not** implemented. Zero-lap sessions are stored as normal sessions.
- The `/api/v1/stats/effort` aggregate endpoint does **not** exist. The frontend's Overview screen
  approximates effort display by counting sessions and laps per time bucket (weekly or monthly bar
  charts) using data fetched from `/api/1/sessions` and `/api/1/laps`.
- The frontend API version is `/api/1` (not `/api/v1`) — all routes should use this prefix.