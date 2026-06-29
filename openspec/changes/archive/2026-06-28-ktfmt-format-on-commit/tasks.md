## 1. Harden the pre-commit hook

- [x] 1.1 Rewrite `.githooks/pre-commit` to capture the staged Kotlin files (`git diff --cached --name-only --diff-filter=ACM` filtered to `*.kt`/`*.kts`), run `./gradlew ktfmtFormat`, then re-stage only those files
- [x] 1.2 Make the hook exit non-zero (abort the commit) if `./gradlew ktfmtFormat` fails
- [x] 1.3 No-op cleanly when no staged Kotlin files exist (let the commit proceed)
- [x] 1.4 Ensure the hook is executable (`chmod +x .githooks/pre-commit`) and committed as such

## 2. Activate the hook via Gradle

- [x] 2.1 Add a Gradle task (root or `app/build.gradle.kts`) that runs `git config core.hooksPath .githooks`
- [x] 2.2 Guard the task to no-op when there is no `.git` directory (CI / source tarball)
- [x] 2.3 Wire the task so a normal build applies it (lifecycle dependency or configuration-time apply)
- [x] 2.4 Run a clean build and confirm `git config core.hooksPath` returns `.githooks`

## 3. Documentation

- [x] 3.1 Document the pre-commit formatting hook in `README.md` / backend docs: what it does, that it activates via the Gradle build, and that `git commit --no-verify` bypasses it

## 4. Verify

- [x] 4.1 Stage a deliberately unformatted `.kt` change, commit, and confirm the committed content is ktfmt-formatted
- [x] 4.2 With one file staged and another modified-but-unstaged, commit and confirm only the staged file was reformatted/re-staged and the unstaged change remains unstaged
- [x] 4.3 Confirm `git commit --no-verify` skips the hook
- [x] 4.4 Run `./gradlew :app:ktfmtCheckMain` to confirm the formatted tree passes the CI check
