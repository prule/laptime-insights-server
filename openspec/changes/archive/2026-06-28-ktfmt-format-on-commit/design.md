## Context

ktfmt (Google style) is configured in `app/build.gradle.kts` via the
`com.ncorti.ktfmt.gradle` plugin, exposing `:app:ktfmtFormat` / `:app:ktfmtCheck*`.
CI runs `./gradlew build`, which invokes `ktfmtCheckMain` and fails on drift.

A `.githooks/pre-commit` already exists and is tracked + executable:

```sh
#!/bin/sh
# Run ktfmt on changed files before committing
./gradlew ktfmtFormat
git add .
```

But it never runs: `core.hooksPath` is unset, so git looks in `.git/hooks/`,
which holds a stale non-executable copy. The hook is also blunt — `git add .`
stages the developer's entire working tree, and there is no failure handling.

## Goals / Non-Goals

**Goals:**
- Make the tracked hook actually run, with zero manual per-clone setup.
- Format only what's being committed; never silently stage unrelated work.
- Abort the commit if formatting can't complete.
- Document the workflow and the `--no-verify` escape hatch.

**Non-Goals:**
- Frontend formatting (separate toolchain; out of scope).
- Replacing the ktfmt CI check — it stays as the backstop.
- Adopting a third-party hook manager (e.g. Husky/pre-commit framework) — keep it
  to a plain shell hook + Gradle.

## Decisions

- **Activate via Gradle, not manual `git config`.** Add a Gradle task that runs
  `git config core.hooksPath .githooks`, wired so a normal build applies it
  (e.g. the task runs on configuration or is a dependency of a common lifecycle
  task). Rationale: a fresh clone is made usable by the build everyone already
  runs; no README step to forget. _Alternatives:_ (a) document a manual
  `git config core.hooksPath .githooks` — rejected, easy to skip, which is the
  exact failure we just hit; (b) copy the hook into `.git/hooks` via a task —
  rejected, `.git/hooks` is untracked and drifts.
  - Guard for non-git checkouts (e.g. source tarball / CI without `.git`): the
    task should no-op if there is no `.git` directory.

- **Re-stage only previously-staged files.** Replace `git add .` with: capture
  the staged file list (`git diff --cached --name-only --diff-filter=ACM`),
  filter to `*.kt`/`*.kts`, run the format task, then `git add` exactly those
  paths. Rationale: a pre-commit hook must not expand the commit's scope.
  _Alternative:_ keep `git add .` — rejected, it pollutes commits with unrelated
  working-tree changes.

- **Fail closed.** If the Gradle format task exits non-zero, the hook exits
  non-zero so git aborts the commit. `--no-verify` remains the deliberate bypass.

- **Keep running the project-level `ktfmtFormat`.** The hook invokes
  `./gradlew ktfmtFormat` (formats the relevant source sets). Scoping to
  individual files is a possible later optimization; correctness first.

## Risks / Trade-offs

- [Hook slows commits — Gradle startup cost] → Acceptable for backend commits;
  developers can `--no-verify` for trivial non-Kotlin commits. Revisit with a
  daemon/scoped run if it bites.
- [Gradle wrapper not on PATH or different CWD] → Hook calls `./gradlew` from the
  repo root (git runs hooks from the top level), matching the existing hook.
- [`core.hooksPath` already set by a developer to something else] → The task sets
  it to `.githooks`; document this so anyone with a custom global hooks path is
  aware. Low likelihood in this repo.
- [No `.git` dir (CI/tarball)] → Task no-ops; CI keeps relying on the ktfmt check.

## Migration Plan

1. Harden `.githooks/pre-commit` (staged-only re-add, failure handling).
2. Add the Gradle wiring to set `core.hooksPath .githooks` (with no-`.git` guard).
3. Run a build once to activate locally; verify a deliberately-unformatted commit
   gets auto-formatted and that an unstaged file stays unstaged.
4. Document in `README.md` / backend docs.

Rollback: unset with `git config --unset core.hooksPath` and revert the commit.

## Open Questions

None — scope is backend Kotlin only.
