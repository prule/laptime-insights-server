## Context

The frontend (`frontend/`, React 19 + Vite 8 + Vitest) is managed with npm. CI (`.github/workflows/build-and-test.yml`) uses `actions/setup-node` with `cache: npm` and runs `npm ci` / `npm test` / `npm run build`. A stray empty `package-lock.json` sits at the repo root with no matching `package.json`. The Kotlin/Gradle backend is unaffected. Local toolchain: Node v24, npm 11, pnpm 11.5 already installed.

## Goals / Non-Goals

**Goals:**
- Switch the frontend to pnpm for install, lockfile, CI, and dev workflows.
- Pin pnpm version reproducibly (Corepack-compatible `packageManager` field).
- Remove npm lockfiles and update all docs/CI references.

**Non-Goals:**
- No dependency version upgrades — same `package.json` deps, just a new lockfile.
- No changes to backend, Gradle, or application runtime code.
- No monorepo/workspace restructuring (`frontend/` stays standalone; no `pnpm-workspace.yaml` needed).

## Decisions

**1. pnpm setup in CI via `pnpm/action-setup` + `setup-node` cache.**
Use `pnpm/action-setup@v4` to install pnpm, then `actions/setup-node@v6` with `cache: pnpm` and `cache-dependency-path: frontend/pnpm-lock.yaml`. Order matters: `pnpm/action-setup` must run before `setup-node` so the cache step finds pnpm. Alternative considered: Corepack (`corepack enable`) — works but Corepack on newer Node has signature/registry friction in CI; the dedicated action is the documented, lower-friction path.

**2. Pin pnpm with `packageManager` field.**
Add `"packageManager": "pnpm@<version>"` to `frontend/package.json`. `pnpm/action-setup` reads this automatically, giving one source of truth for the version locally (Corepack) and in CI. Alternative: hardcode `version:` in the action — rejected, splits the version into two places.

**3. `pnpm install --frozen-lockfile` in CI (npm ci equivalent).**
Fails the build if the lockfile and `package.json` disagree, matching the strictness of `npm ci`.

**4. Delete both lockfiles; commit fresh `pnpm-lock.yaml`.**
Root `package-lock.json` is empty/orphaned → just remove. `frontend/package-lock.json` → replaced by `pnpm-lock.yaml` generated from the unchanged `package.json`.

## Risks / Trade-offs

- **pnpm strict `node_modules` exposes phantom dependencies** (packages imported but not declared). → After `pnpm install`, run `pnpm build` + `pnpm test` locally; add any missing direct deps to `package.json`. Low risk given the small, modern dep set.
- **Resolved dependency versions may shift** vs npm's lockfile (different resolver). → Lockfile is regenerated from the same semver ranges; verify build + tests pass before committing.
- **CI step ordering bug** (setup-node before pnpm) → cache lookup fails. → Place `pnpm/action-setup` first; verify the CI run.
- **Contributors still on npm** could recreate `package-lock.json`. → Document pnpm-only; the `packageManager` field nudges Corepack users to the right tool.

## Migration Plan

1. Add `packageManager` to `frontend/package.json`.
2. `cd frontend && rm package-lock.json && pnpm install` to generate `pnpm-lock.yaml`.
3. `rm` root `package-lock.json`.
4. Update CI workflow, `README.md`, `frontend/README.md`, `docs/frontend-technical.md`.
5. Verify `pnpm build` + `pnpm test` locally and on CI.

Rollback: restore the deleted `package-lock.json` files and revert workflow/docs; npm metadata is untouched in `package.json`.
