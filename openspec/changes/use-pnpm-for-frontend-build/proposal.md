## Why

The frontend is standardized on pnpm (pinned `packageManager`, committed `pnpm-lock.yaml`, pnpm-based CI), but the Gradle frontend tasks in `app/build.gradle.kts` still shell out to `npm install` / `npm run build`. This is inconsistent with the rest of the project, ignores the committed lockfile (risking drift between local/CI/Gradle builds), and requires npm to be installed alongside pnpm.

## What Changes

- Change the `npmInstall` Gradle task to run `pnpm install --frozen-lockfile` against `frontend/`.
- Change the `npmBuild` Gradle task to run `pnpm run build`.
- Rename the tasks to pnpm-oriented names (`pnpmInstall`, `pnpmBuild`) and update `copyFrontend`'s dependency to match, keeping the existing `copyFrontend` -> `dist` -> `static` wiring intact.
- Use the pnpm executable in a cross-platform-safe way (handle `pnpm.cmd` on Windows / commandLine quirks).

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `pnpm-package-management`: Add a requirement that the Gradle frontend build tasks install and build the frontend via pnpm (with a frozen lockfile), not npm.

## Impact

- `app/build.gradle.kts` — the `npmInstall` / `npmBuild` `Exec` tasks.
- Build tooling: requires pnpm on `PATH` for any Gradle path that builds the frontend (already required by CI and local dev). npm no longer needed for the Gradle frontend bundle.
- Docs: `docs/frontend-technical.md` / `README.md` if they describe the Gradle frontend tasks.
- No runtime/API behavior change; output remains `frontend/dist` copied into `app/src/main/resources/static`.
