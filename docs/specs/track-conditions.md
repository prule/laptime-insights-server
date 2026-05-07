# Specification: Environmental Context

## Goal

Provide context to why laptimes might vary (e.g., a "Green" track vs. a rubbered-in track).

## Functional Requirements

- **Condition Snapshot:** Capture `trackGrip`, `rainIntensity`, and `trackTemp` at the start of every lap.
- **Insight Generation:** When displaying lap history, categorize laps by "Dry", "Damp", or "Wet" conditions.

## Business Rules

- If `rainIntensity` > 0.3, the lap is automatically tagged as "Wet Weather Practice".

## Implementation status

**Not implemented.** No environmental data is currently captured or stored.

- `LapTelemetryTable` stores `splinePosition`, `speedKph`, `gear`, `throttle`, and `brake` per sample.
  `trackGrip`, `rainIntensity`, and `trackTemp` are absent from the schema.
- No lap categorisation logic (Dry / Damp / Wet) exists in the backend or frontend.
- Real-time telemetry ingestion via `REALTIME_CAR_UPDATE` is not yet wired into `ClientInitializer`,
  so even the prerequisite data stream is not active.
- This feature remains a future milestone and has no planned delivery date.