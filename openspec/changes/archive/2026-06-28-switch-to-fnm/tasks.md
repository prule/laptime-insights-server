## 1. Version pin files

- [x] 1.1 Rename root `.nvmrc` to `.node-version` (value stays `24.17.0`)
- [x] 1.2 Rename `landing/.nvmrc` to `landing/.node-version` (value stays `24.17.0`)
- [x] 1.3 Add `frontend/.node-version` containing `24.17.0`
- [x] 1.4 Confirm no `.nvmrc` files remain (`grep -r --include='.nvmrc'` / `find . -name .nvmrc`)

## 2. CI

- [x] 2.1 In `.github/workflows/build-and-test.yml`, replace `setup-node`'s inline `node-version: '24'` with `node-version-file: frontend/.node-version`
- [x] 2.2 Confirm no remaining hardcoded Node version in the workflows

## 3. Documentation

- [x] 3.1 Update `README.md` (`.nvmrc` → `.node-version`, `nvm use` → `fnm use`)
- [x] 3.2 Update `frontend/README.md` (same replacements)
- [x] 3.3 Update `landing/README.md` (same replacements)
- [x] 3.4 Update `docs/frontend-technical.md` (same replacements)
- [x] 3.5 Grep the repo to confirm no `nvm`/`.nvmrc` references remain outside `openspec/changes/archive/`

## 4. Verify

- [x] 4.1 Run `fnm use` then `pnpm install` and `pnpm build` in `frontend/` to confirm the pin and engine guard work
- [ ] 4.2 Push and confirm the frontend CI job resolves Node 24 from `node-version-file` and goes green
