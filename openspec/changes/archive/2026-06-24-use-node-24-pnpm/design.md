## Context

The repo has two JS workspaces: `frontend/` (React/Vite, consumed by the Gradle build via `app/build.gradle.kts` pnpm tasks) and `landing/` (static Tailwind marketing site deployed to Cloudflare Pages). Current state:

- Root `.nvmrc` = `24.17.0`.
- `frontend/package.json` and `landing/package.json` both declare `packageManager: pnpm@11.8.0` but **neither declares `engines`**.
- `landing/` has no local `.nvmrc`.
- `.github/workflows/build-and-test.yml` frontend job already uses `pnpm/action-setup@v6` + `actions/setup-node@v6` with `node-version: '24'` and `cache: pnpm`.
- `.github/workflows/react-doctor.yml` does not set up Node/pnpm itself (action-managed).
- Gradle (`app/build.gradle.kts`) shells out to `pnpm install --frozen-lockfile` / `pnpm run build`; it relies on the ambient Node/pnpm.
- No Dockerfiles.

## Goals / Non-Goals

**Goals:**
- Enforce Node 24 + pnpm consistently across `frontend/` and `landing/`.
- Fail fast on the wrong Node/pnpm version (via `engines`).
- Keep CI on Node 24 + pnpm for every JS step; add coverage for `landing/` if it has none.
- Documentation names Node 24 + pnpm.

**Non-Goals:**
- Bumping the pnpm version itself, or upgrading any dependencies.
- Changing the Gradle build's Node/pnpm acquisition strategy (no node-gradle plugin introduction).
- Any runtime/application behavior change.

## Decisions

- **`engines` block in both package.json files.** Use `"node": ">=24"` (range, not exact, to tolerate patch/minor) and `"pnpm": ">=11"` matched to the pinned `packageManager`. Range avoids breaking every contributor on a `24.x` patch mismatch while still rejecting Node 22/older.
- **`engine-strict` warns; a `preinstall` guard enforces.** `engines` is advisory in pnpm. Verified empirically: pnpm 11.8.0 with `engine-strict=true` only prints `[WARN] Unsupported engine` and still completes (exit 0) — it does **not** block a wrong Node. So we keep `engine-strict=true` (`.npmrc`) for the visible warning and npm-compatibility, AND add a tiny `preinstall` script in each `package.json` that checks `process.versions.node` and `process.exit(1)` on Node < 24. That gives a true fail-fast on any real install. (pnpm skips lifecycle scripts on a no-op "already up to date" install — acceptable, since nothing is being installed then; fresh checkouts and CI always run it.)
- **Share the Node pin.** Add a `landing/.nvmrc` (or symlink-equivalent copy) of `24.17.0` so `nvm use` works from that directory; keep the root `.nvmrc` authoritative. Two small files is simpler than tooling to share one.
- **CI: confirm, don't over-engineer.** The frontend job is already correct. Audit other workflows; only `landing/` needs attention if it currently has no build/deploy CI. If landing deploys are manual (`pnpm deploy` via wrangler locally), document Node 24 + pnpm rather than inventing a new workflow — unless the user wants CI for it.
- **Docs.** Update README / `docs/frontend-technical.md` / landing docs prerequisites to "Node 24 + pnpm".

## Risks / Trade-offs

- `engine-strict=true` can block contributors on Node 23/25 if we pin too narrowly; mitigated by `>=24` range.
- The Gradle build uses ambient Node — `engines` won't enforce there unless the install step runs through pnpm with engine-strict, which it does (`pnpm install`). A developer on the wrong Node will now get a clear failure during `:app:pnpmInstall` instead of a confusing build error. Acceptable / desirable.
- Two `.nvmrc` files can drift; mitigated by keeping the version string identical and noting it in docs.
