---
name: vssw:setup-git-hooks
description: >
  Sets up automatic code formatting on commit using Git Hooks.
  Use this skill whenever asked to add pre-commit hooks, formatting on commit, or linting hooks.
---

# Setup Git Hooks Skill

When asked to set up git hooks for formatting or linting, enforce the following pattern using Git's native `core.hooksPath` feature (do not use third-party tools like Husky for this unless explicitly required by a Node-only monorepo).

## 1. Create the Hooks Directory
- Create a `.githooks` directory in the root of the project.
- Create a `.githooks/pre-commit` file.
- The file must be executable (e.g., recommend running `chmod +x .githooks/pre-commit`).

## 2. Define the Pre-Commit Hook
- The `pre-commit` script should run the project's standard formatter (e.g., `./gradlew ktfmtFormat` for Kotlin, or `./run format`) and then re-add the changed files.
- Example:
  ```sh
  #!/bin/sh
  # Format code before committing
  ./gradlew ktfmtFormat
  git add .
  ```

## 3. Register the Hooks Path
- To ensure all developers use the hook, the hook path must be registered locally.
- Instruct the user to add the following command to the project's `./run setup` script (or equivalent bootstrap script):
  `git config core.hooksPath .githooks`
- This guarantees that anyone who sets up the project automatically has the pre-commit hook enabled.
