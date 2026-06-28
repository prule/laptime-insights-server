## Why

The repo pins Node 24 via `.nvmrc` and tells developers to run `nvm use`. `fnm`
(Fast Node Manager) is faster, cross-platform, and supports shell auto-switching
on `cd`. It reads the tool-agnostic `.node-version` file, which is also honoured
by `setup-node` (`node-version-file`) and other tooling. Standardising on
`fnm` + `.node-version` removes the nvm-specific assumption and gives a single
version pin that both the version manager and CI consume.

## What Changes

- Rename the Node version pins from `.nvmrc` to `.node-version` at the repo root
  and in `landing/` (value unchanged: `24.17.0`). `frontend/` resolves the root
  pin today; add an explicit `frontend/.node-version` so each JS workspace is
  self-describing.
- Update CI to drive the Node version from the pin file via
  `setup-node`'s `node-version-file` instead of the hardcoded `node-version: '24'`.
- Update developer docs (`README.md`, `frontend/README.md`, `landing/README.md`,
  `docs/frontend-technical.md`) to reference `fnm use` / `.node-version` instead
  of `nvm use` / `.nvmrc`.

No application code, dependencies, or runtime behaviour changes — Node 24.17.0
remains the pinned version.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `pnpm-package-management`: the Node-version-pin scenarios change from `.nvmrc`
  + `nvm use` to `.node-version` + `fnm use`, and the CI requirement drives the
  Node version from the pin file rather than a hardcoded value.

## Impact

- Files: `.nvmrc` → `.node-version` (root, `landing/`), new `frontend/.node-version`.
- CI: `.github/workflows/build-and-test.yml` (`setup-node` uses `node-version-file`).
- Docs: `README.md`, `frontend/README.md`, `landing/README.md`,
  `docs/frontend-technical.md`.
- Developers must have `fnm` installed instead of (or alongside) `nvm`; `fnm`
  also reads `.node-version`, so the pin stays discoverable. No app or dependency
  changes.
