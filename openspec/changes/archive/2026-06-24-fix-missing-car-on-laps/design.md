## Context

The ACC ingest (`ClientInitializer`) maps each car index to a car model from `EntryListCar` messages,
storing it in `SessionState.carModels`. Laps read the model via `SessionState.getCarModel(carId)` at
`LAPCOMPLETED`, and `CreateLapService` persists `LAP.car` (nullable varchar).

Three issues cause null `car`:

1. **Race** — a lap completes before its car's `EntryListCar` is processed, so `getCarModel` returns
   null and the lap is stored car-less.
2. **Gate** — `buildEntryListCar` only registers a car when `context.focusedCarIndex != null`, so
   registrations that arrive before the focused car is identified are dropped entirely.
3. **No correction** — once `LAP.car` is null it is never updated, even after the model is known.

This mirrors the recently-fixed lap-telemetry linkage problem (frames recorded before the Lap row
existed, then back-filled on completion). The same back-fill shape applies here.

`LAP` already has `session_uid` (varchar), `car_id` (integer) and a nullable `car` column, so the
back-fill is a single targeted UPDATE — no schema change.

## Goals / Non-Goals

**Goals:**
- Every lap ends up with its car model regardless of `EntryListCar` ordering.
- Repair the session's car when it was created before the focused car was known.
- Stop dropping early `EntryListCar` registrations.

**Non-Goals:**
- No schema change; no change to how the model name itself is resolved (`findCarByModel`).
- No one-time migration of already-stored null-car laps in this change (flagged as debt).
- No change to telemetry, comparison, or unrelated ingest paths.

## Decisions

### Back-fill laps via a new out-port + use case (mirror `LinkLapTelemetry*`)

Add `RecordCarOnLapsPort.fillMissingCar(sessionUid, carIndex, car): Int` (out port), a
`RecordCarOnLapsUseCase` + service, and a `LapRepository` method:

```
UPDATE LAP SET car = :car
WHERE session_uid = :sessionUid AND car_id = :carIndex AND car IS NULL
```

Returns the number of laps repaired. Only null-car rows are touched, so it is idempotent and never
overwrites an already-attributed lap. Wire it in `AppModule` and call it from the car-registration
path.

- Alternative considered: back-fill inside `SessionState.registerCar`. Rejected — `SessionState` is
  pure in-memory ingest state with no persistence ports; the write belongs behind a port, invoked by
  `ClientInitializer` after `registerCar`.

### Register the car on every `EntryListCar`, gate only the focused-car session update

Change `buildEntryListCar` so the registration (`sessionState.registerCar` + the lap back-fill) runs
whenever a session is active, independent of `focusedCarIndex`. Keep the existing focused-car branch
(set `session.car` / `playerCarId`) behind the `carId == focusedCarIndex` check. This removes the
drop-everything gate while preserving focused-car behaviour.

### Session car back-fill reuses the existing session update

When the focused car's `EntryListCar` resolves and the session was created car-less, the existing
`UpdateSessionCommand` path already sets `session.car`; ensure it runs even when the session was
started before the car was known (it currently does, once the focused-car branch is reached). No new
port needed for the session.

## Risks / Trade-offs

- [Back-fill UPDATE runs on every EntryListCar] → Cheap (indexed-ish on session + car, null-car only)
  and bounded by laps so far; EntryListCar messages are infrequent. Acceptable.
- [A car that never sends EntryListCar stays null] → Same as today; the fix only helps when the model
  eventually arrives. Out of scope to synthesise a model.
- [Concurrent lap creation vs back-fill] → Both are short transactions; a lap created car-less just
  after a back-fill is repaired on the next registration, and a lap created with the car is left
  untouched by the null-only WHERE clause.

## Migration Plan

Forward-fix only; no schema change. Existing rows already stored with a null `car` are not repaired
by this change — note a one-time backfill (link by `session_uid + car_id`, or re-derive from
`EntryListCar` history if available) in `docs/technical-debt.md`. Rollback = revert the ingest +
port/service additions.

## Open Questions

- Should the lap back-fill also set `track` when it is null (the same race can drop `track`)? Leaning
  no for this change — scope to `car` as reported; revisit `track` separately if it recurs.
