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