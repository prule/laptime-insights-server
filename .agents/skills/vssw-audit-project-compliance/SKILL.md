---
name: vssw:audit-project-compliance
description: >
  Audits the current project to ensure it complies with the Playbook's strict standards.
  Use this skill whenever asked to "audit the project", "check compliance", or "find what's missing".
---

# Audit Project Compliance Skill

When the user asks you to audit the project, perform the following 3 phases. Do not prompt the user for input until Phase 3 is complete.

## Phase 1: Project Type Detection
Use terminal tools to determine the project stack:
- **Frontend Project:** Has a `package.json` (specifically React/Vite).
- **Backend Project:** Has Kotlin/Gradle files.
- **Monorepo:** Has both frontend and backend modules.

## Phase 2: The Compliance Scan
Scan the codebase against these strict criteria.

### Global Checks (All Projects)
1.  **Run Script:** Does a `./run` shell script exist at the root with `setup`, `serve`, and `build` commands?
2.  **Git Hooks:** Does `.githooks/pre-commit` exist? Does `./run setup` run `git config core.hooksPath .githooks`?
3.  **CI/CD:** Does a `.github/workflows` directory exist with a deployment/CI pipeline?

### Frontend Checks (If Frontend)
1.  **Package Manager:** Is `pnpm-lock.yaml` present? (Fail if `package-lock.json` or `yarn.lock` exists).
2.  **PWA:** Is the app configured as a Progressive Web App (PWA)? (Look for Vite PWA plugin or `manifest.json`).
3.  **E2E Tests:** Are Playwright tests configured?
4.  **Testing Pattern:** Do the E2E tests use the Serenity / Screenplay pattern for UI interaction?

### Backend Checks (If Backend)
1.  **Architecture:** Does the directory structure follow Hexagonal Architecture (ports and adapters)?
2.  **Testing:** Are standard unit and integration tests present?

## Phase 3: The Report Artifact
Output your findings as a Markdown Artifact (`project_audit_report.md`):
- Present a clear ✅ / ❌ checklist of the criteria above.
- For every ❌, provide an actionable AI prompt the user can run to fix it. (e.g., *"Ask me to run the `vssw:setup-git-hooks` skill to resolve issue #2."*)
