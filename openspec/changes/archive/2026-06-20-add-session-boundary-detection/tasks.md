## 1. Domain: session end

- [x] 1.1 Add `endedAt: Instant?` to `Session` with an idempotent `end(time)` method and `endedAt()` accessor; update the class KDoc that says lifetime is "bounded by startedAt only"
- [x] 1.2 Add `SessionTest` cases: `end` sets `endedAt`, ending an already-ended session does not corrupt the recorded end

## 2. Application: EndSession use case

- [x] 2.1 Add `EndSessionCommand` (session uid + end time) and `EndSessionUseCase` port in `application/port/in/session`
- [x] 2.2 Add `EndSessionService` in `application/domain/service/session` implementing the use case; wire it in `AppModule`
- [x] 2.3 Add `EndSessionServiceTest`

## 3. Persistence: ended_at

- [x] 3.1 Add migration in `db/migrations/` adding nullable `ended_at` column to the session table
- [x] 3.2 Add the column to `SessionTable`/`SessionEntity` and map it in `SessionMapper`; persist end via `SessionPersistenceAdapter`
- [x] 3.3 Update `SessionRepositoryTest` to cover persisting and reading `ended_at`

## 4. SessionTracker (boundary logic)

- [x] 4.1 Create `SessionTracker` that, given observed `(sessionIndex, sessionType, phase)`, returns a boundary decision: `Start`, `Continue`, `EndThenStart`, or `End` — pure, no I/O
- [x] 4.2 Implement rules: start on `PRE_SESSION`/`SESSION` when inactive; `EndThenStart` when active and identity key differs; `End` on terminal phase (`SESSION_OVER`/`POST_SESSION`/`RESULT_UI`); otherwise `Continue`
- [x] 4.3 Add `SessionTrackerTest` covering first session, second-race identity change, terminal-phase end, restart after end, and non-session phases

## 5. Wire into ingestion

- [x] 5.1 In `ClientInitializer`, replace the single-shot start block with `SessionTracker` decisions: create/start on `Start`, finalize via `EndSessionUseCase` on `End`, finalize-then-create on `EndThenStart`
- [x] 5.2 Record the active identity key when a session starts; clear active session on `End`
- [x] 5.3 Replace `SessionState` with a fresh instance on every new session start so lap counts/validity reset per session

## 6. Docs & cleanup

- [x] 6.1 Update `docs/real-time-updates.md` and `docs/user-guide.md` to describe session segmentation (identity key + terminal phases)
- [x] 6.2 Run `./gradlew :app:build :app:test`; mark any resolved items in `docs/technical-debt.md` as DONE
