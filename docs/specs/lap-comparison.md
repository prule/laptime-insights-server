# Specification: Lap Comparison Engine

## Goal

Allow users to overlay telemetry data from two different laps to identify performance gaps in gear selection and speed.

## Functional Requirements

- **Spatial Mapping:** Telemetry must be stored indexed by `splinePosition` (0.0 to 1.0) rather than just time, to allow
  1:1 spatial comparison.
- **Key Metrics:** Store `speed`, `gear`, `throttle`, and `brake` at a minimum frequency of 10Hz.
- **Reference Lap:** Users must be able to designate a "Personal Best" (PB) as the baseline for comparison.

## Comparison Logic

- **Speed Delta:** Calculate the difference in KPH at every 1% of the track length.
- **Gear Variance:** Highlight segments where the gear index differs between Lap A and Lap B.

## Proposed API Endpoint

`GET /api/v1/laps/compare?lapId1={id}&lapId2={id}`

## Implementation status

- Telemetry persisted in `LAP_TELEMETRY` (one row per sample, indexed by
  `lap_uid` and `spline_position`) — see
  `app/.../adapter/out/persistence/lap/LapTelemetryTable.kt`.
- Endpoints implemented under `/api/1` (the project's versioning), not
  `/api/v1`:
  - `GET /api/1/laps/{uid}/telemetry` — full sample list for one lap.
  - `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…` — both laps' raw samples
    plus lap metadata in a single payload, with HATEOAS `_links`.
- The spec calls for "speed delta at every 1% of track length"; the backend
  returns **raw** samples and the React client resamples to 100 buckets
  (`SpeedDeltaTrace`). Moving the resample server-side is a one-method change
  in `CompareLapsService` if that becomes useful.
- Throttle and brake are stored as 0.0–1.0 floats. Gear is an int (0 = neutral,
  -1 = reverse, ≥1 forward).
- Synthetic samples are written by `DatabaseSeeder` so the compare flow
  works end-to-end without a live `acc-client` feed. Real ingestion via
  `REALTIME_CAR_UPDATE` is not yet wired into `ClientInitializer`.

## Lap telemetry

Comes in REALTIME_CAR_UPDATE messages and looks like the following:

```json
{
  "carIndex": 5,
  "driverIndex": 0,
  "driverCount": 1,
  "gear": 2,
  "worldPosX": 609.7051,
  "worldPosY": 165.99849,
  "yaw": 0.3873835,
  "carLocation": "TRACK",
  "kmh": 24,
  "position": 6,
  "cupPosition": 2,
  "trackPosition": 0,
  "splinePosition": 0.81604,
  "laps": 0,
  "delta": 0,
  "bestSessionLap": {
    "lapTimeMs": 2147483647,
    "carIndex": 5,
    "driverIndex": 0,
    "numSplits": 3,
    "splits": [
      2147483647,
      2147483647,
      2147483647
    ],
    "isInvalid": 0,
    "isValidForBest": 1,
    "isOutlap": 0,
    "isInlap": 0
  },
  "lastLap": {
    "lapTimeMs": 2147483647,
    "carIndex": 5,
    "driverIndex": 0,
    "numSplits": 0,
    "splits": [],
    "isInvalid": 0,
    "isValidForBest": 1,
    "isOutlap": 0,
    "isInlap": 0
  },
  "currentLap": {
    "lapTimeMs": 0,
    "carIndex": 5,
    "driverIndex": 0,
    "numSplits": 0,
    "splits": [],
    "isInvalid": 0,
    "isValidForBest": 1,
    "isOutlap": 0,
    "isInlap": 0
  }
}
```