## Context

Node 24.17.0 is pinned via `.nvmrc` at the repo root and in `landing/`;
`frontend/` has no pin of its own and resolves the root file. Docs tell
developers to run `nvm use`. CI (`build-and-test.yml`) hardcodes
`node-version: '24'` in `setup-node`. This is a small, mechanical toolchain
swap: same Node version, different version manager and pin filename.

## Goals / Non-Goals

**Goals:**
- Pin Node via the tool-agnostic `.node-version` file in every JS workspace.
- Standardise developer instructions on `fnm`.
- Have CI read the Node version from the pin file (single source of truth).

**Non-Goals:**
- Changing the Node version (stays `24.17.0`).
- Changing pnpm, dependencies, or any application/runtime behaviour.
- Forcing removal of `nvm` from developer machines — `fnm` also reads
  `.node-version`, and so does `nvm` (>= 0.39), so the pin stays usable either way.

## Decisions

- **`.node-version` over keeping `.nvmrc`.** `.node-version` is honoured by
  `fnm`, `setup-node` (`node-version-file`), Volta, and nodenv, whereas `.nvmrc`
  is nvm-specific. One filename serves the version manager and CI. _Alternative:_
  keep `.nvmrc` (fnm reads it too) — rejected because the proposal's intent is to
  drop the nvm-specific convention and align the pin filename with the tooling.
- **Add `frontend/.node-version`.** Today `frontend/` relies on the root pin.
  Making each workspace self-describing means `fnm use` and `setup-node`'s
  `cache-dependency-path`/`node-version-file` work the same from any directory.
- **CI uses `node-version-file: frontend/.node-version`.** Replaces the inline
  `node-version: '24'` so a future Node bump only edits the pin files.
  _Alternative:_ leave `node-version: '24'` — rejected; it would drift from the
  pin and defeats the single-source-of-truth goal.

## Risks / Trade-offs

- [Developer without `fnm` installed] → Docs note `fnm` as the recommended
  manager; `.node-version` is also read by `nvm` (>= 0.39) and others, so no one
  is hard-blocked. No enforcement change beyond docs.
- [CI `node-version-file` path wrong / file missing] → Caught immediately by the
  CI run failing to resolve a Node version; verified by the first PR build.
- [Stale `.nvmrc` left behind] → A spec scenario asserts no `.nvmrc` remains;
  grep during implementation confirms removal.

## Migration Plan

1. Rename `.nvmrc` → `.node-version` (root, `landing/`); add `frontend/.node-version`.
2. Point CI `setup-node` at `node-version-file`.
3. Update docs.
4. Verify: `pnpm install` + build in `frontend/`, CI green on the PR.

Rollback is trivial — revert the commit; no data or runtime state involved.

## Open Questions

None.
