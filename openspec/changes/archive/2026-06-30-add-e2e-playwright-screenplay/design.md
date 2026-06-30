## Context

The `frontend/` app (React 19 + Vite 8 + TS 6, pnpm) has Vitest unit/component tests but no browser-level E2E. The app supports two data modes: **MOCK** (in-memory handler mirroring the backend seeder) and **LIVE** (real HTTP to the Ktor API on :8000). Mode is part of every TanStack Query key. MOCK mode means the full app — routing, query layer, all six screens — can run in a browser with zero backend, which makes it the natural target for deterministic E2E. CI already has a `frontend` job (pnpm install → lint → test → build). This design adds Playwright as the runner and Serenity/JS as the authoring layer, per the proposal and `frontend-e2e-testing` spec.

## Goals / Non-Goals

**Goals:**
- Deterministic, backend-free E2E that drives the real app in a browser (MOCK mode).
- Screenplay-pattern authoring (Actors/Tasks/Questions) via `@serenity-js/playwright-test`, not raw locator scripts.
- Cover primary navigation flows across Overview, Sessions, Session Detail, Laps, Compare, Live.
- A CI gate that installs browsers and runs the suite, retaining reports/traces on failure.

**Non-Goals:**
- Testing against the LIVE backend or real ACC telemetry (separate, non-deterministic concern).
- Visual-regression / screenshot-diff testing.
- Cross-browser matrix beyond Chromium initially.
- Replacing existing Vitest unit/component tests.

## Decisions

### D1: Playwright as the E2E runner — `@playwright/test`
Industry-standard, first-class TS, auto-waiting, trace viewer, `webServer` integration to boot the app. **Alternatives:** Cypress (heavier, weaker multi-tab/TS-Screenplay story); WebdriverIO (more config). Playwright pairs cleanly with Serenity/JS.

### D2: Screenplay via Serenity/JS Playwright Test integration
Use `@serenity-js/playwright-test` (custom `test` fixture providing a configured `actor`), `@serenity-js/web` (web interactions: `Navigate`, `Click`, `Text`, `PageElement`, `By`), and `@serenity-js/assertions` (`Ensure`, `equals`, `isPresent`). Tests read as `actorCalled('Driver').attemptsTo(NavigateToOverview(), Ensure.that(...))`. **Alternative:** hand-rolled Page Objects — rejected; the spec mandates Screenplay and Serenity gives reporting + a reusable Task/Question library for free.

### D3: Run in MOCK mode — no backend, no flake
Force the app into MOCK at server boot rather than mocking HTTP at the browser. Playwright's `webServer` starts the app (`pnpm dev`/`preview`) with the env that selects MOCK mode. **Alternatives:** Playwright route interception (re-implements the mock the app already ships — duplication); running the real backend (slow, non-deterministic seed/telemetry). Confirm during implementation which env var / config selects MOCK and set it in `webServer.env`; default to `pnpm preview` against a built bundle for speed and CI realism, falling back to `pnpm dev` if HMR isn't a concern.

### D4: Directory & config layout
- `frontend/playwright.config.ts` — `testDir: './e2e'`, Chromium project, `webServer` (MOCK), `reporter` including the Serenity/JS reporter + Playwright HTML, `trace: 'on-first-retry'`.
- `frontend/e2e/` — `specs/` (test files), `screenplay/` (reusable Tasks & Questions), optional `serenity/` fixtures.
- Keep E2E out of the Vitest glob (Vitest `include` is `src/**/*.test.{ts,tsx}`, so `e2e/**` is already excluded — verify, no overlap).

### D5: Stable selectors via roles + `data-testid`
Screenplay Questions target ARIA roles/headings first; add `data-testid` hooks only where role/text is ambiguous (e.g. the session row to drill into, the laps table). Minimal, additive component edits.

### D6: CI integration
Extend the `frontend` job (or add a parallel `e2e` job) in `build-and-test.yml`: after `pnpm install --frozen-lockfile`, run `pnpm exec playwright install --with-deps chromium`, then `pnpm e2e`. Upload `playwright-report/` and Serenity report as artifacts on failure. Cache the Playwright browser download keyed on the Playwright version to keep runs fast.

### D7: Scripts
`package.json`: `"e2e": "playwright test"`, `"e2e:ui": "playwright test --ui"`, `"e2e:report": "playwright show-report"`.

## Risks / Trade-offs

- **MOCK selection mechanism unconfirmed** → During implementation, locate how the app picks MOCK vs LIVE and drive it from `webServer.env`; if it's build-time only, build a MOCK bundle for `preview`.
- **Serenity/JS + Playwright version coupling** → Pin compatible versions per Serenity/JS docs; CI `--frozen-lockfile` keeps it reproducible.
- **CI time / flake from browser download** → Cache browsers keyed on version; Chromium-only initially; `trace: on-first-retry` plus 1 retry in CI to absorb rare boot races.
- **Selector brittleness** → Prefer roles/headings; confine `data-testid` to genuinely ambiguous nodes; centralize in Questions so a markup change is a one-file fix.
- **Advisory vs blocking gate** → Land advisory-capable but default to blocking the check on failure (the spec requires failures to block); revisit if the suite proves flaky.

## Migration Plan

1. Add dev deps + `playwright.config.ts` + `e2e/` scaffold + scripts; `pnpm exec playwright install chromium` locally.
2. Build the Screenplay library (navigation Tasks, screen-content Questions) and the navigation-flow specs.
3. Wire CI (browser install + `pnpm e2e` + artifact upload).
4. Update `docs/frontend-technical.md` (testing strategy) and mark the E2E item DONE in `docs/technical-debt.md`.
- **Rollback:** additive only — remove the `e2e/` dir, config, scripts, and the CI step; no app/runtime behavior depends on it.

## Open Questions

- Exact env var / config key that forces MOCK mode at runtime (resolve by reading `src/config` and the data-mode provider during implementation).
- `pnpm preview` (built bundle) vs `pnpm dev` for the Playwright `webServer` — default to `preview` for CI realism unless a blocker surfaces.
- Make the CI E2E gate blocking immediately, or advisory for an initial stabilization window? (Spec wants blocking; confirm with maintainer.)
