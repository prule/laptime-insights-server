## 1. Tooling & Dependencies

- [x] 1.1 Add dev deps to `frontend/package.json`: `@playwright/test`, `@serenity-js/core`, `@serenity-js/playwright`, `@serenity-js/playwright-test`, `@serenity-js/web`, `@serenity-js/assertions` (pin compatible Serenity/JS versions); run `pnpm install`.
- [x] 1.2 Install the Chromium browser locally: `pnpm exec playwright install chromium`.
- [x] 1.3 Add scripts to `package.json`: `"e2e": "playwright test"`, `"e2e:ui": "playwright test --ui"`, `"e2e:report": "playwright show-report"`.
- [x] 1.4 Add `playwright-report/`, `test-results/`, and the Serenity report output dir to `frontend/.gitignore`.

## 2. Determine MOCK-mode boot

- [x] 2.1 Read `frontend/src/config` and the data-mode provider to confirm the exact env var / config key that forces MOCK mode (resolves design Open Question). **Finding:** there is no env var — `DataModeProvider` defaults to `"mock"` (`DEFAULTS.mode`), reading an override only from `localStorage["lti.dataMode"]`. A fresh Playwright browser context has empty `localStorage`, so the app boots in MOCK with zero backend. No `webServer.env` needed.
- [x] 2.2 Verify a MOCK-mode app boots with no backend via `pnpm preview` (built bundle) or `pnpm dev`; pick the one used by Playwright's `webServer`. **Decision:** use `pnpm preview` against a built bundle for CI realism + speed; Playwright `webServer` runs `pnpm build && pnpm preview`.

## 3. Playwright + Serenity config

- [x] 3.1 Create `frontend/playwright.config.ts`: `testDir: './e2e/specs'`, Chromium project, `webServer` that boots the app in MOCK mode (env from 2.1), `trace: 'on-first-retry'`, 1 retry in CI. **Note:** MOCK needs no env — fresh browser context defaults to it; `webServer` runs `pnpm build && pnpm preview`.
- [x] 3.2 Configure reporters: Serenity/JS reporter (`@serenity-js/playwright-test`) plus Playwright HTML reporter. (Plus `list` + `ArtifactArchiver` + `Photographer` for failure screenshots.)
- [x] 3.3 Confirm `e2e/**` does not overlap the Vitest `include` glob (`src/**/*.test.{ts,tsx}`); adjust if needed so the two suites stay separate. **Confirmed:** Vitest only globs `src/**`, and `e2e/` is outside tsconfig `include`, so the two never collide.

## 4. Screenplay interaction library

- [x] 4.1 Create `frontend/e2e/screenplay/` with reusable navigation Tasks for each main screen (Overview, Sessions, Session Detail, Laps, Compare, Live). (`tasks.ts`: `OpenTheApp`, `NavigateToScreen`, `SelectAllTimeRange`, `OpenTheFirstSession`.)
- [x] 4.2 Create Questions for reading screen content (heading/role, key data region, laps table rows). (`questions.ts`: `TheScreenTitle`, `TheNumberOfSessions`, `TheNumberOfLaps`.)
- [x] 4.3 Add minimal `data-testid` (or rely on ARIA roles/headings) on the ambiguous nodes only — the session row to drill into and the laps table. (Added: per-screen root testids, `screen-title`, `session-row`, `laps-table`, `lap-row`, `time-range-*`.)

## 5. E2E specs (Screenplay)

- [x] 5.1 `e2e/specs/navigation.spec.ts`: Actor navigates to each main screen; assert each renders its primary heading + key data region without runtime errors.
- [x] 5.2 `e2e/specs/session-detail.spec.ts`: Actor opens Sessions, selects a session, asserts Session Detail renders for that session including its laps table.
- [x] 5.3 Run `pnpm e2e` locally; confirm green and that tests use Actors/Tasks/Questions (no raw locators inlined in test bodies). **7 passed in 8.7s.**

## 6. CI integration

- [x] 6.1 In `.github/workflows/build-and-test.yml`, add a step/job that runs `pnpm exec playwright install --with-deps chromium` then `pnpm e2e` (after install) for the frontend. (Added a dedicated `e2e` job.)
- [x] 6.2 Cache Playwright browsers keyed on the Playwright version to keep runs fast. (`actions/cache` on `~/.cache/ms-playwright` keyed on resolved Playwright version; OS deps installed even on cache hit.)
- [x] 6.3 Upload `playwright-report/` and the Serenity report as artifacts on failure. (`actions/upload-artifact` gated on `failure()`.)
- [x] 6.4 Confirm a failing E2E test fails the CI check (gate is blocking). **Blocking by default:** `pnpm e2e` exits non-zero on failure (verified locally) and the step has no `continue-on-error`.

## 7. Docs & cleanup

- [x] 7.1 Document the E2E + Screenplay testing strategy (how to run, where tests live, MOCK-mode rationale) in `docs/frontend-technical.md`. (New "## Testing" section.)
- [x] 7.2 Mark the frontend E2E/Playwright/Screenplay gap as DONE in `docs/technical-debt.md`.
- [x] 7.3 Update `frontend/README.md` with the `pnpm e2e` workflow and the `playwright install` setup step. (New "## Testing" section.)
