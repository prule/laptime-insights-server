# pnpm-package-management

## Purpose

Define how the frontend manages JavaScript dependencies, scripts, CI, and documentation using pnpm as the package manager.

## Requirements

### Requirement: Frontend uses pnpm as package manager

The frontend SHALL use pnpm for dependency installation and lockfile management. The repository SHALL NOT contain `package-lock.json` files, and `frontend/package.json` SHALL declare pnpm via a `packageManager` field.

#### Scenario: Install dependencies

- **WHEN** a developer runs `pnpm install` in `frontend/`
- **THEN** dependencies install successfully and a `frontend/pnpm-lock.yaml` is present

#### Scenario: No npm lockfiles remain

- **WHEN** the repository is inspected after migration
- **THEN** no `package-lock.json` exists at the repo root or in `frontend/`
- **AND** `frontend/package.json` contains a `packageManager` field pinning a pnpm version

### Requirement: Frontend scripts run via pnpm

All documented frontend developer commands SHALL be runnable through pnpm.

#### Scenario: Run dev, build, and test

- **WHEN** a developer runs `pnpm dev`, `pnpm build`, or `pnpm test` in `frontend/`
- **THEN** each command executes the corresponding script and completes successfully

### Requirement: CI installs and builds with pnpm

The CI workflow SHALL install dependencies and run the frontend build/test using pnpm with a frozen lockfile and pnpm-based caching.

#### Scenario: CI frontend job

- **WHEN** the frontend CI job runs
- **THEN** it sets up pnpm, restores a pnpm cache keyed on `pnpm-lock.yaml`, installs with `pnpm install --frozen-lockfile`, and runs `pnpm test` and `pnpm build`
- **AND** the job fails if `pnpm-lock.yaml` is out of date with `package.json`

### Requirement: Documentation references pnpm

Project documentation SHALL instruct developers to use pnpm commands for the frontend.

#### Scenario: Docs show pnpm commands

- **WHEN** a developer reads `README.md`, `frontend/README.md`, or `docs/frontend-technical.md`
- **THEN** setup and build instructions use `pnpm install` / `pnpm <script>` and no `npm` commands remain
