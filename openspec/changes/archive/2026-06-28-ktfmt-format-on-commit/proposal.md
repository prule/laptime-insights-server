## Why

Backend Kotlin is formatted with ktfmt, and CI fails the build on any
unformatted file (`:app:ktfmtCheckMain`). Formatting recently drifted on `main`
and only surfaced in CI, forcing a separate cleanup. A `.githooks/pre-commit`
script that runs `./gradlew ktfmtFormat` already exists in the repo, but nothing
activates it: `core.hooksPath` is unset, so git uses the stale, non-executable
copy in `.git/hooks/` and the hook never runs. Developers should get
auto-formatting locally so unformatted code never reaches CI.

## What Changes

- Wire up the tracked `.githooks/` directory so the pre-commit hook actually
  runs — configure `core.hooksPath` automatically as part of the Gradle build
  (no manual per-clone setup).
- Harden the pre-commit hook so it:
  - runs the ktfmt Gradle format task,
  - re-stages only the files that were already staged (not a blanket `git add .`),
  - aborts the commit if formatting fails.
- Document the hook (what it does, how it activates, how to bypass with
  `--no-verify`) in `README.md` / backend docs.

No application code or runtime behaviour changes.

## Capabilities

### New Capabilities
- `kotlin-formatting`: how backend Kotlin is auto-formatted with ktfmt — the
  Gradle format/check tasks, the activated pre-commit git hook, and the
  developer-setup contract that keeps local and CI formatting consistent.

### Modified Capabilities
<!-- none -->

## Impact

- Files: `.githooks/pre-commit` (hardened), Gradle build wiring to set
  `core.hooksPath` (`app/build.gradle.kts` or root build script), docs
  (`README.md` and/or `docs/`).
- Developer workflow: commits to backend Kotlin are auto-formatted; bypass with
  `git commit --no-verify`.
- No new runtime dependencies; ktfmt is already configured.
