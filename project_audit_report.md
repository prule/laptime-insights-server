# Project Audit Report

**Project:** laptime-insights-server
**Type detected:** 🔀 **Monorepo** (React/Vite frontend + Kotlin/Gradle backend)
**Date:** 2026-06-30

---

## Global Checks (All Projects)

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| 1 | `./run` script with `setup`, `serve`, `build` | ✅ | `run` at root implements all three commands |
| 2 | `.githooks/pre-commit` exists & `./run setup` sets `core.hooksPath` | ✅ | `.githooks/pre-commit` (ktfmt); `run setup` runs `git config core.hooksPath .githooks` |
| 3 | `.github/workflows` CI/CD pipeline | ✅ | `build-and-test.yml`, `react-doctor.yml` |

## Frontend Checks (React/Vite)

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| 1 | pnpm (no npm/yarn lockfile) | ✅ | `frontend/pnpm-lock.yaml` only; no `package-lock.json`/`yarn.lock` |
| 2 | Configured as a PWA | ➖ N/A | **Intentional non-goal** — self-hosted, backend-dependent, desktop-only app shipped as a release bundle. See `docs/architecture.md` §6. |
| 3 | Playwright E2E tests | ❌ | No `playwright` dependency or `playwright.config.*`; only Vitest unit tests |
| 4 | E2E uses Serenity / Screenplay pattern | ❌ | No E2E layer exists (blocked by #3) |

## Backend Checks (Kotlin/Gradle)

| # | Criterion | Status | Evidence |
|---|-----------|--------|----------|
| 1 | Hexagonal Architecture (ports & adapters) | ✅ | `adapter/in/web`, `adapter/out/{persistence,event}`, `application/port/{in,out}`, `application/domain/{model,service}` |
| 2 | Unit & integration tests present | ✅ | 17 test files: domain unit tests + repository/controller integration tests |

---

## Remediation Prompts (for each ❌)

### Frontend #2 — PWA (resolved: N/A)
> Marked an intentional non-goal — a PWA is not suitable for a self-hosted, backend-dependent, desktop-only app distributed as a release bundle. Rationale recorded in `docs/architecture.md` §6. No action required.

### Frontend #3 — Add Playwright E2E
> "Set up Playwright end-to-end testing in the frontend: add the `@playwright/test` dependency, create `playwright.config.ts`, scaffold an `e2e/` test directory, and wire an E2E script into `package.json` and the CI workflow."

### Frontend #4 — Adopt Serenity / Screenplay pattern
> "Once Playwright is set up, refactor the E2E tests to use the Serenity/Screenplay pattern — model Actors, Tasks, Questions, and Interactions rather than direct page-object/locator calls."

---

## Summary

**7 / 9 applicable passing** (PWA excluded as an intentional non-goal). The backend and all global tooling are fully compliant. The remaining gap is the frontend **E2E** story: there is no Playwright (and therefore no Screenplay) layer. Unit testing (Vitest) is in place, but browser-level coverage is missing.
