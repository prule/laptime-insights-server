## MODIFIED Requirements

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
