## 1. Core fix — SessionTracker

- [x] 1.1 In `app/.../SessionTracker.kt`, change the `current == null` branch of `observe()` to start a session for any phase that is **not** terminal (return `Start` unless `phase.isTerminalPhase()`, in which case `Continue`).
- [x] 1.2 Keep `isTerminalPhase()` as-is; `isStartPhase()` is no longer used by the open path — remove it if it becomes dead, or retain only if referenced elsewhere.
- [x] 1.3 Add a debug/info log of `(sessionIndex, sessionType, phase) -> decision` in `ClientInitializer.buildRealTimeUpdate` (or in the tracker call site) to confirm phase sequences against real telemetry.

## 2. Tests

- [x] 2.1 Update `SessionTrackerTest.kt`: replace the "non-session phase before any session does not start" expectation — `STARTING`/`FORMATION_LAP` while no session active now return `Start`.
- [x] 2.2 Keep/confirm "terminal phase before any session is ignored" still returns `Continue`.
- [x] 2.3 Add a scenario: joining mid-stream at a non-start, non-terminal phase opens a session and a following `LAPCOMPLETED`-equivalent identity is attributed to it.
- [x] 2.4 Re-run existing multi-session and identity-change tests to confirm no regression.

## 3. Docs

- [x] 3.1 Update `docs/real-time-updates.md` session-segmentation section: a session now starts on any non-terminal phase when none is active (not only `PRE_SESSION`/`SESSION`); explain this captures practice/qualifying laps.
- [x] 3.2 If any cleanup surfaced (e.g. dead `isStartPhase`), note/mark it in `docs/technical-debt.md`.

## 4. Verify

- [x] 4.1 Run `./gradlew :app:test` — all green.
- [x] 4.2 Against a captured/live practice→qualifying→race weekend, confirm sessions of all three types now have laps recorded.
