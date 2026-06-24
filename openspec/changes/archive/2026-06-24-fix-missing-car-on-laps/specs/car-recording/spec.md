## ADDED Requirements

### Requirement: Laps are attributed a car model regardless of message ordering

Every lap SHALL be attributed the car model of the car that drove it, even when the lap completes
before that car's `EntryListCar` message has been processed. When the car model is known at lap
creation it SHALL be stored then; when it is not, the lap SHALL be corrected once the car model
becomes known.

#### Scenario: Car known at lap completion

- **WHEN** a lap completes and the car's model has already been registered
- **THEN** the lap is stored with that car model

#### Scenario: Lap completes before the car is known, car arrives later

- **WHEN** a lap completes for a car whose model is not yet registered
- **AND** the car's `EntryListCar` is processed afterward
- **THEN** the lap's car model is back-filled to the resolved model
- **AND** laps for other cars or other sessions are not changed

#### Scenario: Already-attributed laps are not overwritten

- **WHEN** a car model is registered
- **THEN** only laps for that session and car index whose car is currently null are updated
- **AND** laps that already have a car model keep it

### Requirement: Early entry-list registrations are not dropped

The ingest SHALL register a car's model whenever its `EntryListCar` message arrives during an active
session, rather than ignoring registrations that arrive before the focused (player) car index is
known. The focused car SHALL still additionally update the session's car and player car index.

#### Scenario: EntryListCar before the focused car is identified

- **WHEN** an `EntryListCar` arrives while the focused car index is not yet known
- **AND** a session is active
- **THEN** the car model is registered for that car index

#### Scenario: Focused car still updates the session

- **WHEN** the `EntryListCar` for the focused car is processed
- **THEN** the session's car model and player car index are set from it

### Requirement: Session car is recorded when it becomes known

When the session was created before the focused car's model was known, the session's car SHALL be
back-filled once that car's `EntryListCar` is processed, so the session is not left with a null car.

#### Scenario: Session created before the car is known

- **WHEN** a session is created with no car model
- **AND** the focused car's `EntryListCar` is processed afterward
- **THEN** the session's car model is updated to the resolved model
