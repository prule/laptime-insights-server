## Why

The frontend has unit/component coverage (Vitest) but no browser-level end-to-end tests, so regressions in routing, data fetching, and screen rendering can only be caught by hand. A compliance audit flagged the gap (Playwright + Serenity/Screenplay). Adding deterministic E2E tests that drive the real app in a browser closes it and gives us a safety net for user-facing flows.

## What Changes

- Add **Playwright** as the E2E browser runner in `frontend/`, configured to launch the Vite app in **MOCK** data mode so tests are deterministic and need no backend or live ACC telemetry.
- Adopt the **Serenity/JS Screenplay** pattern (`@serenity-js/playwright-test`, `@serenity-js/web`, `@serenity-js/assertions`) — tests are written as Actors performing Tasks and asking Questions, not raw locator scripts.
- Add an `e2e/` test layer covering the primary navigation flows across the existing screens (Overview, Sessions, Session Detail, Laps, Compare, Live).
- Add `pnpm e2e` (and `e2e:ui`) scripts and a `playwright.config.ts`; wire Playwright browser install + an E2E job into CI (`build-and-test.yml`).
- Update docs: record the E2E/Screenplay approach in `docs/frontend-technical.md`, and mark the E2E item DONE in `docs/technical-debt.md`.

## Capabilities

### New Capabilities
- `frontend-e2e-testing`: Browser-level end-to-end testing of the React frontend using Playwright, authored with the Serenity/JS Screenplay pattern, run against the app in MOCK mode and gated in CI.

### Modified Capabilities
<!-- None — no existing spec requirements change; this adds a new test capability only. -->

## Impact

- **Dependencies (dev):** `@playwright/test`, `@serenity-js/core`, `@serenity-js/playwright`, `@serenity-js/playwright-test`, `@serenity-js/web`, `@serenity-js/assertions`, Playwright browser binaries (Chromium).
- **Frontend:** new `frontend/e2e/` directory, `playwright.config.ts`, `package.json` scripts, `.gitignore` entries for `playwright-report/`, `test-results/`. Screens/components may need stable `data-testid`/role hooks for Screenplay Questions.
- **CI:** `.github/workflows/build-and-test.yml` frontend job (or a new `e2e` job) installs Playwright browsers and runs `pnpm e2e`.
- **Docs:** `docs/frontend-technical.md` (testing strategy), `docs/technical-debt.md` (mark E2E gap DONE).
- No backend, API, or runtime app behavior changes.
