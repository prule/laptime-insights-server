## Why

Laps (and sometimes the session) are intermittently recorded with **no car model** — `car` is null.
The car model for each ACC car index is only known once that car's `EntryListCar` message has been
processed, but laps can complete before then, and there is no correction afterward:

- `SessionState.getCarModel(carId)` returns null until the car's `EntryListCar` has registered it.
- `buildEntryListCar` **ignores all `EntryListCar` messages while `context.focusedCarIndex` is null**,
  dropping registrations that arrive before the focused car is identified.
- When a lap completes before its car is registered, `CreateLapService` stores `car = null` and it is
  **never back-filled** once the `EntryListCar` finally arrives.

The result is laps with a missing car, which breaks car filtering, the "Same car" comparison filter,
and per-car stats. (This is exactly the null-`car` data observed on real laps.)

## What Changes

- **Back-fill the car model onto laps once it becomes known.** When a car is registered (its
  `EntryListCar` resolves), update any already-persisted laps for that session + car index whose
  `car` is null, setting them to the resolved model. Order-independent, mirroring the lap-telemetry
  linkage fix.
- **Back-fill the session car** for the focused (player) car when it resolves, if the session was
  created before the car was known.
- **Stop dropping early `EntryListCar` registrations**: register the car model whenever an
  `EntryListCar` arrives and a session is active, instead of gating on `focusedCarIndex` being known
  (still special-case the focused car for `session.car` / `playerCarId`).
- Records the car at lap-creation time when already known (unchanged); the back-fill only repairs the
  cases where it wasn't.

## Capabilities

### New Capabilities
- `car-recording`: How the ACC ingest attributes a car model to each lap and to the session,
  including correcting laps recorded before the car's `EntryListCar` was processed.

### Modified Capabilities
<!-- None: no existing spec covers car attribution in the ingest pipeline. -->

## Impact

- **Backend ingest**: `ClientInitializer.buildEntryListCar` (relax the focused-car gate; trigger the
  back-fill on registration), `SessionState` (expose registered car ids if needed). New out-port +
  use case + service to update laps' car by session + car index (mirrors `LinkLapTelemetry*`), plus a
  repository method. Possibly reuse the existing session update for the session car.
- **Data**: no schema change — `LAP.car` is already nullable. Existing laps already stored with a null
  car need a one-time back-fill migration (note in `docs/technical-debt.md`); the fix corrects new
  occurrences going forward.
- **Tests**: ingest/persistence test for the back-fill (lap recorded car-less, then car registered →
  lap gets the car).
- **Docs**: `docs/technical-debt.md` (historical back-fill) and any ingest/architecture doc that
  describes car attribution.
