## 1. Back-fill port + persistence

- [x] 1.1 Add `LapRepository.fillMissingCar(sessionUid, carIndex, car): Int` — `UPDATE LAP SET car = :car WHERE session_uid = :sessionUid AND car_id = :carIndex AND car IS NULL`, returning the row count.
- [x] 1.2 Add out-port `RecordCarOnLapsPort.fillMissingCar(sessionUid, carIndex, car): Int` and implement it on `LapPersistenceAdapter` (wrapped in a transaction).

## 2. Use case + wiring

- [x] 2.1 Add in-port `RecordCarOnLapsUseCase` + `RecordCarOnLapsService` that calls the out-port.
- [x] 2.2 Wire the port/use case in `AppModule` (Lap module).

## 3. Ingest fix (ClientInitializer)

- [x] 3.1 In `buildEntryListCar`, register the car (`sessionState.registerCar`) whenever a session is active, independent of `context.focusedCarIndex` — drop the focused-car gate on registration.
- [x] 3.2 After registering, call `RecordCarOnLapsUseCase` to back-fill any car-less laps for that session + car index.
- [x] 3.3 Keep the focused-car branch (set `session.car` / `playerCarId`) so the session car is recorded/back-filled when the focused car resolves.

## 4. Tests & docs

- [x] 4.1 Persistence test: a lap stored with null car is updated by `fillMissingCar` for the matching session + car index, while non-null and non-matching laps are untouched (idempotent).
- [x] 4.2 Note the one-time historical back-fill of existing null-car laps in `docs/technical-debt.md`.
- [x] 4.3 Run `./gradlew :app:test` (and frontend checks if any UI copy changed); all green.
