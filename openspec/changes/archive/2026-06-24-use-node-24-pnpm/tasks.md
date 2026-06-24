## 1. Enforce Node 24 + pnpm in package.json

- [x] 1.1 Add `engines` to `frontend/package.json`: `"node": ">=24"`, `"pnpm": ">=11"`.
- [x] 1.2 Add `engines` to `landing/package.json`: `"node": ">=24"`, `"pnpm": ">=11"`.
- [x] 1.3 Add `engine-strict=true` via `frontend/.npmrc` and `landing/.npmrc` (or `pnpm.engineStrict` in package.json) so the Node check is enforced at install.

## 2. Node version pin

- [x] 2.1 Add `landing/.nvmrc` with `24.17.0` (matching root `.nvmrc`).
- [x] 2.2 Confirm root `.nvmrc` is `24.17.0` and reachable from `frontend/`.

## 3. CI

- [x] 3.1 Confirm `.github/workflows/build-and-test.yml` frontend job sets up Node 24 + pnpm with `--frozen-lockfile` (already present — verified).
- [x] 3.2 Audit all other workflows (`react-doctor.yml`, any others) for JS install/build steps that run without Node 24 + pnpm setup; fix any found. (None: react-doctor uses an action that manages its own runtime; backend job runs no JS; Gradle `copyFrontend` is not wired into `build`.)
- [x] 3.3 If `landing/` has no build/deploy CI, decide with the user whether to add a Node 24 + pnpm workflow or document the manual `pnpm deploy` flow. (Decision: document the manual flow — see Task 5.2.)

## 4. Verification

- [x] 4.1 Run `pnpm install --frozen-lockfile` in `frontend/` and `landing/` on Node 24; confirm success. (Both exit 0.)
- [x] 4.2 Confirm install fails with an engine error on a Node version < 24 (e.g. via `nvm use 22`). (Note: `engine-strict` only WARNs in pnpm 11.8.0; added a `preinstall` guard that hard-fails — verified `ELIFECYCLE` exit 1 on a fresh Node 22 install. See design.md.)
- [x] 4.3 Run `./gradlew :app:pnpmBuild` to confirm the Gradle-driven frontend build still passes. (BUILD SUCCESSFUL.)

## 5. Documentation

- [x] 5.1 Update `README.md` and `docs/frontend-technical.md` prerequisites to state Node 24 + pnpm.
- [x] 5.2 Update landing docs/README to state Node 24 + pnpm. (Manual `pnpm deploy` flow already documented in `landing/README.md`.)
- [x] 5.3 Remove any remaining `npm install` / `npm run` references in docs in favor of pnpm. (Docs already pnpm-only; no `npm install`/`npm run` found.)
