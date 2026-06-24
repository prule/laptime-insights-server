## Why

Node and pnpm versions are pinned in some places but not enforced or applied consistently. The `frontend/` package uses pnpm and a root `.nvmrc` declares Node 24, but neither `package.json` declares an `engines` constraint, the standalone `landing/` package has no Node version pin, and only the frontend has CI that runs on Node 24 + pnpm. This lets contributors (and CI for `landing/`) drift onto other Node or package-manager versions, producing "works on my machine" failures.

## What Changes

- Declare `engines.node` (`>=24`) and `engines.pnpm` in both `frontend/package.json` and `landing/package.json` so the wrong Node/pnpm version is caught at install time.
- Ensure a Node version pin (`.nvmrc` / shared `24.17.0`) is honored by every JS workspace, including `landing/`.
- Verify and, where missing, standardize CI to set up Node 24 + pnpm before any JS build/install step (frontend job already does this; confirm no other workflow installs JS deps on a different runtime).
- Update documentation (README, `docs/frontend-technical.md`, landing docs) to state Node 24 + pnpm as the required toolchain.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `pnpm-package-management`: Extend beyond "frontend uses pnpm" to require an enforced Node 24 toolchain and pnpm usage across all JS workspaces (`frontend/` and `landing/`), declared via `engines` and a Node version pin, and applied consistently in CI and docs.

## Impact

- `frontend/package.json`, `landing/package.json` — add `engines` block.
- `.nvmrc` / `landing/` — Node version pin reachable from the landing workspace.
- `.github/workflows/*.yml` — confirm Node 24 + pnpm setup for any JS step.
- `README.md`, `docs/frontend-technical.md`, landing docs — toolchain instructions.
- No application/runtime behavior changes; tooling and CI only.
