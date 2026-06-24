## ADDED Requirements

### Requirement: JS workspaces pin and enforce Node 24

Every JavaScript workspace in the repository SHALL target Node 24 and SHALL declare it so the wrong Node version is caught at install time. This applies to `frontend/` and `landing/`.

#### Scenario: package.json declares engines

- **WHEN** `frontend/package.json` or `landing/package.json` is inspected
- **THEN** each contains an `engines` field requiring `node` `>=24` and pinning `pnpm`

#### Scenario: Node version pin is discoverable

- **WHEN** a developer runs `nvm use` (or an equivalent version manager) from any JS workspace
- **THEN** Node 24 (`24.17.0`) is selected via a discoverable `.nvmrc`

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

Every CI workflow step that installs or builds JavaScript SHALL set up Node 24 and pnpm before running, with pnpm-based caching.

#### Scenario: Frontend CI job runs on Node 24

- **WHEN** the frontend CI job runs
- **THEN** it sets up pnpm and Node 24, restores a pnpm cache keyed on `pnpm-lock.yaml`, and installs with `pnpm install --frozen-lockfile`

#### Scenario: No CI step installs JS deps on a different runtime

- **WHEN** the CI workflows are inspected
- **THEN** no workflow installs JavaScript dependencies without first setting up Node 24 and pnpm

### Requirement: Documentation states the Node 24 + pnpm toolchain

Project documentation SHALL instruct developers that the required toolchain is Node 24 and pnpm for all JS workspaces.

#### Scenario: Docs name Node 24 and pnpm

- **WHEN** a developer reads `README.md`, `docs/frontend-technical.md`, or the landing docs
- **THEN** the prerequisites state Node 24 and pnpm, and no `npm` install commands remain
