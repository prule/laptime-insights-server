# kotlin-formatting

## Purpose

Define how backend Kotlin is auto-formatted with ktfmt — the Gradle format/check tasks, the activated pre-commit git hook, and the developer-setup contract that keeps local and CI formatting consistent.

## Requirements

### Requirement: Backend Kotlin is formatted with ktfmt

Backend Kotlin sources SHALL be formatted with ktfmt (Google style) via Gradle. The build SHALL expose a format task and a check task, and CI SHALL fail when sources are not properly formatted.

#### Scenario: Format task reformats sources

- **WHEN** a developer runs `./gradlew :app:ktfmtFormat`
- **THEN** all backend Kotlin sources are reformatted to ktfmt Google style in place

#### Scenario: Check task gates unformatted code

- **WHEN** `./gradlew :app:ktfmtCheckMain` runs in CI against unformatted sources
- **THEN** the task fails and the build does not pass

### Requirement: A pre-commit hook auto-formats staged Kotlin

The repository SHALL provide a version-controlled pre-commit git hook that runs the ktfmt Gradle format task before a commit completes, so unformatted Kotlin cannot be committed. The hook SHALL re-stage the files it reformats that were already staged, and SHALL NOT stage files the developer had not staged.

#### Scenario: Hook formats staged code on commit

- **WHEN** a developer commits a staged Kotlin file that is not ktfmt-formatted
- **THEN** the pre-commit hook runs the ktfmt format task, re-stages the reformatted file, and the resulting commit contains properly formatted code

#### Scenario: Hook does not stage unrelated changes

- **WHEN** the working tree contains modified-but-unstaged files at commit time
- **THEN** the hook reformats and re-stages only files that were already staged
- **AND** the unstaged changes remain unstaged

#### Scenario: Hook aborts on formatting failure

- **WHEN** the ktfmt format task fails (e.g. the build cannot run)
- **THEN** the hook exits non-zero and the commit is aborted

#### Scenario: Hook can be bypassed deliberately

- **WHEN** a developer runs `git commit --no-verify`
- **THEN** the pre-commit hook is skipped and the commit proceeds without formatting

### Requirement: The hook activates without manual per-clone setup

The pre-commit hook SHALL be activated automatically through the Gradle build by pointing git at the tracked hooks directory, so a fresh clone does not require each developer to install the hook by hand.

#### Scenario: Build configures the hooks path

- **WHEN** a developer runs a Gradle build in a fresh clone
- **THEN** git `core.hooksPath` is set to the tracked `.githooks` directory
- **AND** subsequent commits run the tracked pre-commit hook

### Requirement: The hook workflow is documented

Project documentation SHALL describe the pre-commit formatting hook: what it does, that it activates via the Gradle build, and how to bypass it.

#### Scenario: Docs describe the hook

- **WHEN** a developer reads the backend setup documentation
- **THEN** it states that commits auto-format Kotlin via a pre-commit hook, that the hook activates through the Gradle build, and that `git commit --no-verify` bypasses it
