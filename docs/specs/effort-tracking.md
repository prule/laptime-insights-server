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