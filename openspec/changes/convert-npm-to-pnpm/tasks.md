## 1. Lockfile migration

- [x] 1.1 Add `"packageManager": "pnpm@<version>"` to `frontend/package.json` (use the local pnpm version, e.g. `pnpm@11.5.2`)
- [x] 1.2 Delete `frontend/package-lock.json`
- [x] 1.3 Run `cd frontend && pnpm install` to generate `frontend/pnpm-lock.yaml`
- [x] 1.4 Delete the stray root `package-lock.json`

## 2. Verify build

- [x] 2.1 Run `pnpm build` in `frontend/`; fix any phantom-dependency errors by adding missing direct deps to `package.json`
- [x] 2.2 Run `pnpm test` in `frontend/` and confirm it passes

## 3. CI workflow

- [x] 3.1 In `.github/workflows/build-and-test.yml` frontend job, add a `pnpm/action-setup@v4` step before the Node setup step
- [x] 3.2 Change `setup-node` to `cache: pnpm` and `cache-dependency-path: frontend/pnpm-lock.yaml`
- [x] 3.3 Replace `npm ci` with `pnpm install --frozen-lockfile`
- [x] 3.4 Replace `npm test` with `pnpm test` and `npm run build` with `pnpm build`

## 4. Documentation

- [x] 4.1 Update `README.md` (lines ~39-40) to pnpm commands
- [x] 4.2 Update `frontend/README.md` (lines ~10, 64-76) to pnpm commands
- [x] 4.3 Update `docs/frontend-technical.md` (lines ~183, 196-197) to pnpm commands
- [x] 4.4 N/A — no npm/lockfile entry exists in `docs/technical-debt.md`

## 5. Final verification

- [x] 5.1 Confirm no `package-lock.json` remains anywhere in the repo (excluding `node_modules`)
- [ ] 5.2 Push and confirm the CI frontend job passes with pnpm
