# Specification: Lap Comparison Engine

## Goal

Allow users to overlay telemetry data from two different laps to identify performance gaps in gear selection and speed. The comparison should additionally let users relate chart features to physical track position via an interactive track map.

## Functional Requirements

- **Spatial Mapping:** Telemetry must be stored indexed by `splinePosition` (0.0 to 1.0) rather than just time, to allow
  1:1 spatial comparison.
- **Key Metrics:** Store `speedKph`, `gear`, `worldPosX`, and `worldPosY` from ACC `REALTIME_CAR_UPDATE` messages.
  Throttle and brake are not available from the ACC UDP broadcasting protocol and are therefore not stored.
- **Reference Lap:** Users must be able to designate a "Personal Best" (PB) as the baseline for comparison.
- **Track Map:** Render a 2-D track outline from `worldPosX`/`worldPosY` coordinates. A dot per lap should track the
  current hover position so users can relate chart features to physical track location.
- **Synchronized Crosshair:** Hovering over any telemetry chart should snap a crosshair to the same `splinePosition`
  across all charts simultaneously, and move the track-map dots accordingly.

## Comparison Logic

- **Speed Delta:** Calculate the difference in KPH at every 1% of the track length.
- **Gear Variance:** Highlight segments where the gear index differs between Lap A and Lap B.

## API Endpoints

Implemented under `/api/1` (the project's versioning):

- `GET /api/1/laps/{uid}/telemetry` — full `TelemetrySample` list for one lap.
- `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…` — both laps' raw samples plus lap metadata in a single payload, with
  HATEOAS `_links`.

The original spec proposed `/api/v1/laps/compare?lapId1=…&lapId2=…`; the implementation uses `lap1Uid`/`lap2Uid` as
parameter names and UID strings rather than numeric IDs.

## TelemetrySample shape

Each sample returned by the API is a `TelemetrySample`:

| Field | Type | Description |
|-------|------|-------------|
| `splinePosition` | `Double` (0.0–1.0) | Position along the lap (0 = start/finish line) |
| `speedKph` | `Double` | Vehicle speed in km/h |
| `gear` | `Int` | Gear index: 0 = neutral, −1 = reverse, ≥1 = forward gear |
| `worldPosX` | `Float` | ACC world-space X coordinate in metres |
| `worldPosY` | `Float` | ACC world-space Y coordinate in metres |

## Implementation status

- Telemetry persisted in `REALTIME_CAR_UPDATE` (one row per sample per car update, indexed by
  `lap_uid` and `spline_position`). The former `LAP_TELEMETRY` table has been removed.
  See `app/.../adapter/out/persistence/car/RealtimeCarUpdateTable.kt`.
- `findByLapUid` in `RealtimeCarUpdateRepository` projects the relevant columns into `TelemetrySample`,
  ordered by `splinePosition` ascending.
- The spec calls for "speed delta at every 1% of track length"; the backend returns raw samples and the
  React client resamples to 100 buckets (`SpeedDeltaTrace`). Moving the resample server-side is a
  one-method change in `CompareLapsService` if that becomes useful.
- Gear is an int (0 = neutral, −1 = reverse, ≥1 forward). Throttle and brake are absent — the ACC UDP
  protocol does not include them in `REALTIME_CAR_UPDATE` messages.
- Synthetic samples are written by `DatabaseSeeder` so the compare flow works end-to-end without a live
  `acc-client` feed. World coordinates are synthesised as a parametric track shape (base ellipse +
  sinusoidal perturbations keyed to the track's corner count) so the track map renders a recognisably
  distinct outline per track.
- Live ingestion via `REALTIME_CAR_UPDATE` messages is wired in `ClientInitializer`
  (`buildRealTimeCarUpdate()`). During live recording the `lapId`/`lapUid` FK on each row is initially
  null; it is set once the lap completes and the FK is known.
- Track map (`TrackMap.tsx`) normalises world coordinates into a square SVG viewBox and renders each
  series' world path as a semi-transparent outline. A coloured dot per lap moves to the nearest sample
  when the parent's `hoveredPosition` changes. Start/finish is marked with a white rectangle.
- All telemetry charts (`TelemetryTrace`, `SpeedDeltaTrace`, `GearMismatchStrip`) and `TrackMap` share
  `hoveredPosition` / `onHover` props. State is lifted to `CompareScreen`.

## Raw ACC REALTIME_CAR_UPDATE message

The fields stored in the database come from this ACC UDP message:

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
  "bestSessionLap": { "lapTimeMs": 2147483647, ... },
  "lastLap": { "lapTimeMs": 2147483647, ... },
  "currentLap": { "lapTimeMs": 0, ... }
}
```

Note: `position` is stored as `racePosition` in the database (`RACE_POSITION` column) to avoid the H2
SQL reserved keyword `POSITION`.
