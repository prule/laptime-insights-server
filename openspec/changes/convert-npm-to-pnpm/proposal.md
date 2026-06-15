## Why

The frontend uses npm for dependency management and CI. Migrating to pnpm gives faster installs, a strict non-flat `node_modules` (no phantom dependencies), and disk savings via the content-addressable store. A stray empty `package-lock.json` at the repo root also indicates npm tooling drift worth cleaning up.

## What Changes

- Replace npm with pnpm as the package manager for `frontend/`.
- Generate `pnpm-lock.yaml`; remove `frontend/package-lock.json`.
- Remove the stray root `package-lock.json` (empty, no matching `package.json`).
- Pin pnpm via `packageManager` field in `frontend/package.json` (Corepack-compatible).
- Update CI (`.github/workflows/build-and-test.yml`): use `pnpm/action-setup`, switch `cache: npm` → `cache: pnpm`, `package-lock.json` → `pnpm-lock.yaml`, `npm ci` → `pnpm install --frozen-lockfile`, `npm test`/`npm run build` → `pnpm test`/`pnpm build`.
- Update docs (`README.md`, `frontend/README.md`, `docs/frontend-technical.md`) to use `pnpm` commands.
- No application code or runtime behavior changes.

## Capabilities

### New Capabilities

- `pnpm-package-management`: Frontend dependency install, lockfile, CI caching, and developer commands run on pnpm instead of npm.

### Modified Capabilities

<!-- None — no existing spec requirements change. -->

## Impact

- **Files**: `frontend/package.json` (add `packageManager`), `frontend/package-lock.json` (delete), root `package-lock.json` (delete), `frontend/pnpm-lock.yaml` (new), `.github/workflows/build-and-test.yml`, `README.md`, `frontend/README.md`, `docs/frontend-technical.md`.
- **CI**: requires pnpm available on the runner (`pnpm/action-setup` or Corepack).
- **Developers**: must use `pnpm` locally; npm commands no longer supported for the frontend.
- **Runtime/app code**: none.
