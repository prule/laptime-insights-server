## ADDED Requirements

### Requirement: Browser-based E2E test harness

The frontend SHALL provide a Playwright-based end-to-end test harness that launches the real React application in a browser and drives it as a user would. The harness SHALL run the app in MOCK data mode so tests are deterministic and require neither the backend API nor live ACC telemetry.

#### Scenario: E2E suite runs against the app in MOCK mode

- **WHEN** a developer runs `pnpm e2e` in `frontend/`
- **THEN** Playwright starts the Vite-served app in MOCK data mode and executes the E2E suite against it in a real browser
- **AND** the run completes without depending on the Kotlin backend or any external network service

#### Scenario: Browsers are provisioned before the run

- **WHEN** the E2E suite is invoked in an environment without Playwright browsers installed
- **THEN** the documented setup step (`pnpm exec playwright install`) provisions the required browser before tests execute

### Requirement: Screenplay-pattern test authoring

E2E tests SHALL be authored using the Serenity/JS Screenplay pattern. Tests SHALL express behaviour through Actors performing Tasks and asking Questions, rather than issuing raw Playwright locator calls inside test bodies.

#### Scenario: A test is written as an Actor performing Tasks

- **WHEN** a new E2E test is added
- **THEN** it is expressed as an Actor (e.g. `actorCalled('Driver')`) performing named Tasks and asserting on Questions
- **AND** low-level page interaction (locators, clicks, waits) is encapsulated in reusable Screenplay Tasks/Questions, not inlined in the test

#### Scenario: Reusable interaction library exists

- **WHEN** multiple tests need to navigate the app or read screen content
- **THEN** they reuse shared Screenplay Tasks and Questions (e.g. navigation, reading a screen heading/table) rather than duplicating locator logic

### Requirement: Coverage of primary navigation flows

The E2E suite SHALL cover the primary user-facing navigation flows across the application's main screens: Overview, Sessions, Session Detail, Laps, Compare, and Live.

#### Scenario: User navigates between main screens

- **WHEN** the Actor opens the app and navigates to each main screen
- **THEN** each screen renders its expected primary content (heading and key data region) without runtime errors

#### Scenario: User drills from a session into its detail

- **WHEN** the Actor opens the Sessions screen and selects a session
- **THEN** the Session Detail screen renders for that session, including its laps table

### Requirement: E2E suite gated in CI

The continuous integration pipeline SHALL run the E2E suite on every pull request and push to `main`, installing the required Playwright browsers, so E2E regressions block merges once the gate is active.

#### Scenario: CI executes E2E on a pull request

- **WHEN** a pull request that touches `frontend/` is opened or updated
- **THEN** CI installs Playwright browsers and runs `pnpm e2e`
- **AND** a failing E2E test causes the CI check to fail

#### Scenario: Test artifacts are retained on failure

- **WHEN** an E2E test fails in CI
- **THEN** the Playwright/Serenity report and traces are available as build artifacts for diagnosis
