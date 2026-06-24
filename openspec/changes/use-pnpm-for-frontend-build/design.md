## Context

`app/build.gradle.kts` defines three `Exec`/`Copy` tasks to bundle the frontend into the backend's static resources:

- `npmInstall` → `commandLine("npm", "install")` in `frontend/`
- `npmBuild` (dependsOn `npmInstall`) → `commandLine("npm", "run", "build")`
- `copyFrontend` (dependsOn `npmBuild`) → copies `frontend/dist` into `app/src/main/resources/static`

The frontend itself is pnpm-only: `frontend/package.json` pins `packageManager: pnpm@11.8.0`, `frontend/pnpm-lock.yaml` is committed, and CI uses pnpm. The npm-based Gradle tasks are the last npm holdout and can produce a different dependency tree than the committed lockfile.

The `copyFrontend` wiring into `processResources` is currently commented out; this change does not alter that — only the package manager used by the install/build tasks.

## Goals / Non-Goals

**Goals:**
- Gradle frontend tasks use pnpm with the committed lockfile.
- No `npm` invocation remains in the Gradle build.
- Preserve existing task wiring (`dist` → `static`).

**Non-Goals:**
- Re-enabling the commented-out `processResources` dependency.
- Replacing the raw `Exec` approach with a Gradle Node plugin.
- Changing CI (already pnpm) or frontend scripts.

## Decisions

**Use `pnpm install --frozen-lockfile`** rather than plain `pnpm install`. Rationale: matches CI, fails fast on lockfile drift, and makes the Gradle build reproducible. Alternative (`pnpm install`) was rejected because it can silently mutate the lockfile during a backend build.

**Rename `npmInstall`/`npmBuild` → `pnpmInstall`/`pnpmBuild`.** Rationale: task name should not lie about the tool. `copyFrontend` updated to `dependsOn("pnpmBuild")`. Alternative (keep names) rejected as misleading.

**Cross-platform pnpm invocation.** On Windows the executable is `pnpm.cmd`. Resolve via `Os.isFamily(Os.FAMILY_WINDOWS)` (or `org.gradle.internal.os.OperatingSystem`) and pick `pnpm`/`pnpm.cmd` accordingly, mirroring how the prior npm tasks would have needed it. Keeps the build runnable on dev Windows machines.

## Risks / Trade-offs

- [pnpm not on PATH for a Gradle-only environment] → pnpm is already required by CI and local frontend dev; document the requirement and let the task fail with a clear error if missing.
- [`--frozen-lockfile` fails if lockfile stale] → intended behavior; surfaces drift early. Developer runs `pnpm install` in `frontend/` to refresh, same as CI.
