## ADDED Requirements

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
