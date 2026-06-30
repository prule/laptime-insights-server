## 1. Config: identity + persisted toggle

- [x] 1.1 Add a `PublicProfileConfig` (enabled: Boolean = false; identity: name, slug, tagline, location, memberSince, season, sim) and a `publicProfile` field on `ApplicationConfiguration`.
- [x] 1.2 Add a **write** path to `JsonFileConfigurationRepository` (re-serialize the JSON5 config file atomically: write-temp-then-rename).
- [x] 1.3 Introduce a single mutable config holder (live `ApplicationConfiguration`) wired through `module()`/`AppModule` so handlers read the current value and the toggle writer updates it.

## 2. Feature gating (runtime)

- [x] 2.1 Add `PUBLIC_PROFILE("public-profile", "FEATURE_PUBLIC_PROFILE", defaultEnabled = false)` to `Feature`.
- [x] 2.2 Resolve `public-profile`'s enabled state from `config.publicProfile.enabled` (not the env var) at **request time**; keep other features env-resolved.
- [x] 2.3 Compute the index `enabledFeatures` + profile `_links` in the `GET /api/1` handler from the live config so a runtime toggle is reflected without restart (add a controller test toggling between requests).

## 3. Snapshot generation (application + domain)

- [x] 3.1 Add a domain `ProfileSnapshot` model (+ sub-models for meta/profile/totals/perTrack/records) and an inbound port `BuildProfileSnapshotUseCase`.
- [x] 3.2 Add/extend outbound read ports for the aggregates needed (laps per track; totals; best season + all-time lap per (track, car) over valid laps with date). Reuse existing aggregate queries where they fit; add focused Exposed queries for gaps.
- [x] 3.3 Implement the use case: compose identity/meta from config + totals/perTrack/records from the read ports. Exclude invalid laps from bests; exclude raw telemetry entirely.

## 4. Web adapter (endpoints + links)

- [x] 4.1 Add `ProfileSnapshotResource` (serializes to the frontend `ProfileData` shape) + mapper from `ProfileSnapshot`.
- [x] 4.2 Add `GET /api/1/public-profile` controller returning the snapshot; 404 when disabled.
- [x] 4.3 Add `PUT /api/1/public-profile/enabled` action that sets the toggle, persists it, and returns the new state.
- [x] 4.4 Add `public-profile` (data) + `publicProfileToggle` (action) links to `IndexLinkFactory`, present per enabled state; register controllers in `App.kt`.

## 5. Frontend: dynamic + gated

- [x] 5.1 `api/profile.ts`: make `useProfileData()` a TanStack Query keyed by `[publicProfile, mode, apiUrl]` that follows `_links["public-profile"]` in LIVE; keep `SAMPLE_PROFILE` as the MOCK fixture.
- [x] 5.2 `PublicProfileScreen`: render fetched data with loading + error states (reuse `LoadingState`/`ErrorState`).
- [x] 5.3 Remove `STATICALLY_ENABLED` from `FeaturesProvider`; drop the fake `rel` handling so `public-profile` gates on `enabledFeatures` like other features.
- [x] 5.4 Add a toggle control that calls the `publicProfileToggle` action and refreshes feature/index state.

## 6. Tests

- [x] 6.1 Backend: snapshot builder unit/integration test (H2) — totals, per-track laps, season vs all-time bests, invalid-lap exclusion.
- [x] 6.2 Backend: index/controller tests — links + `enabledFeatures` present only when enabled; toggle persists and is reflected on the next request; snapshot 404 when disabled.
- [x] 6.3 Frontend: update/extend `PublicProfileScreen` tests for fetched data (MOCK fixture) + loading/error; assert no static enable (disabled ⇒ hidden).
- [x] 6.4 Run `./gradlew :app:ktfmtFormat`, `./gradlew :app:test`; `pnpm typecheck && pnpm lint && pnpm test`.

## 7. Docs & cleanup

- [x] 7.1 Update `docs/public-profile.md` (now backend-generated + runtime toggle) and `docs/architecture.md` (config-persisted runtime feature toggle, request-time index resolution).
- [x] 7.2 Update `docs/user-guide.md` with how to enable/disable the public profile and set identity.
- [x] 7.3 Mark the static-enabled-nav item **DONE** in `docs/technical-debt.md`; note add-cloud-publish should upload this snapshot.
