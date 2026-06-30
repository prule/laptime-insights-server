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

### Requirement: JS workspaces pin and enforce Node 24

Every JavaScript workspace in the repository SHALL target Node 24 and SHALL declare it so the wrong Node version is caught at install time. This applies to `frontend/` and `landing/`. The version SHALL be pinned in a tool-agnostic `.node-version` file (read by `fnm` and `setup-node`), not `.nvmrc`.

#### Scenario: package.json declares engines

- **WHEN** `frontend/package.json` or `landing/package.json` is inspected
- **THEN** each contains an `engines` field requiring `node` `>=24` and pinning `pnpm`

#### Scenario: Node version pin is discoverable

- **WHEN** a developer runs `fnm use` from any JS workspace (repo root, `frontend/`, or `landing/`)
- **THEN** Node 24 (`24.17.0`) is selected via a discoverable `.node-version` file
- **AND** no `.nvmrc` file remains in the repository

#### Scenario: Installing on an unsupported Node version fails fast

- **WHEN** a developer runs `pnpm install` in `frontend/` or `landing/` using a Node version older than 24
- **THEN** the install reports an engine mismatch rather than silently proceeding

### Requirement: Landing workspace uses pnpm

The standalone `landing/` workspace SHALL use pnpm for dependency installation and lockfile management, consistent with the frontend.

#### Scenario: Landing installs with pnpm

- **WHEN** a developer runs `pnpm install` in `landing/`
- **THEN** dependencies install successfully and a `landing/pnpm-lock.yaml` is present
- **AND** `landing/package.json` declares pnpm via a `packageManager` field
- **AND** no `package-lock.json` or `yarn.lock` exists in `landing/`

### Requirement: CI uses Node 24 and pnpm for all JS steps

Every CI workflow step that installs or builds JavaScript SHALL set up Node 24 and pnpm before running, with pnpm-based caching. The Node version SHALL be sourced from the `.node-version` pin file via `setup-node`'s `node-version-file`, not a hardcoded version, so CI and local tooling share a single source of truth.

#### Scenario: Frontend CI job runs on Node 24 from the pin file

- **WHEN** the frontend CI job runs
- **THEN** it sets up pnpm and Node via `setup-node` configured with `node-version-file` pointing at a `.node-version` file, restores a pnpm cache keyed on `pnpm-lock.yaml`, and installs with `pnpm install --frozen-lockfile`
- **AND** no CI step hardcodes the Node version inline

#### Scenario: No CI step installs JS deps on a different runtime

- **WHEN** the CI workflows are inspected
- **THEN** no workflow installs JavaScript dependencies without first setting up Node 24 and pnpm

### Requirement: Documentation states the Node 24 + pnpm toolchain

Project documentation SHALL instruct developers that the required toolchain is Node 24 and pnpm for all JS workspaces, and SHALL reference `fnm` + `.node-version` as the way to select Node.

#### Scenario: Docs name Node 24 and pnpm

- **WHEN** a developer reads `README.md`, `docs/frontend-technical.md`, or the landing docs
- **THEN** the prerequisites state Node 24 and pnpm, and no `npm` install commands remain

#### Scenario: Docs reference fnm and .node-version

- **WHEN** a developer reads `README.md`, `frontend/README.md`, `landing/README.md`, or `docs/frontend-technical.md`
- **THEN** Node selection is described via `fnm use` and the `.node-version` pin
- **AND** no `nvm use` or `.nvmrc` references remain

### Requirement: Gradle frontend build uses pnpm

The Gradle frontend build tasks in `app/build.gradle.kts` SHALL install and build the frontend using pnpm. They SHALL NOT invoke `npm`. Dependency installation SHALL use a frozen lockfile so the committed `frontend/pnpm-lock.yaml` is the source of truth.

#### Scenario: Gradle installs frontend dependencies with pnpm

- **WHEN** the Gradle frontend install task runs
- **THEN** it executes `pnpm install --frozen-lockfile` in `frontend/`
- **AND** it does not invoke `npm`
- **AND** the build fails if `pnpm-lock.yaml` is out of date with `package.json`

#### Scenario: Gradle builds the frontend bundle with pnpm

- **WHEN** the Gradle frontend build task runs
- **THEN** it executes `pnpm run build` in `frontend/` after the install task
- **AND** the produced `frontend/dist` output is copied into `app/src/main/resources/static`

#### Scenario: No npm invocations remain in the Gradle build

- **WHEN** `app/build.gradle.kts` is inspected
- **THEN** the frontend tasks reference pnpm and no `commandLine("npm", ...)` invocation remains
