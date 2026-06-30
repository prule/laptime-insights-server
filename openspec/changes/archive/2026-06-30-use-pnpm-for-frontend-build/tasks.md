## 1. Update Gradle frontend tasks

- [x] 1.1 In `app/build.gradle.kts`, add a cross-platform pnpm executable resolver (`pnpm` / `pnpm.cmd` on Windows)
- [x] 1.2 Rename `npmInstall` → `pnpmInstall`, change `commandLine` to `pnpm install --frozen-lockfile`
- [x] 1.3 Rename `npmBuild` → `pnpmBuild` (dependsOn `pnpmInstall`), change `commandLine` to `pnpm run build`
- [x] 1.4 Update `copyFrontend` to `dependsOn("pnpmBuild")`
- [x] 1.5 Confirm no `commandLine("npm", ...)` remains in the file

## 2. Verify

- [x] 2.1 Run `./gradlew :app:pnpmBuild` and confirm pnpm installs (frozen) and builds `frontend/dist`
- [x] 2.2 Run `./gradlew :app:copyFrontend` and confirm `dist` lands in `app/src/main/resources/static`

## 3. Docs

- [x] 3.1 Update `docs/frontend-technical.md` / `README.md` if they reference the npm-based Gradle tasks (none referenced the npm Gradle tasks — already pnpm)
- [x] 3.2 Mark any related item in `docs/technical-debt.md` DONE if present (no related item)
