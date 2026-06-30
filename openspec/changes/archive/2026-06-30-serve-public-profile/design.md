## Context

[add-public-profile] shipped the Driver Passport page in the React app, driven by a bundled
`SAMPLE_PROFILE` constant and force-shown via a `STATICALLY_ENABLED` set in `FeaturesProvider`
(with a fake `public-profile` HATEOAS `rel`). That shortcut is logged in `docs/technical-debt.md`.

The backend is Ktor + Exposed, hexagonal. The HATEOAS entry point `GET /api/1`
([IndexController.kt](app/src/main/kotlin/com/github/prule/laptimeinsights/adapter/in/web/index/IndexController.kt))
returns `_links` (capabilities, always present) + `enabledFeatures` (UI surfaces). Features are
declared in [Feature.kt](app/src/main/kotlin/com/github/prule/laptimeinsights/Feature.kt) and
resolved **once at startup** from `FEATURE_<NAME>` env vars / in-code defaults
([EnvironmentVariables.enabledFeatures()](app/src/main/kotlin/com/github/prule/laptimeinsights/EnvironmentVariables.kt)).
App config is a JSON5 file loaded by `JsonFileConfigurationRepository` into
[ApplicationConfiguration](app/src/main/kotlin/com/github/prule/laptimeinsights/ApplicationConfiguration.kt)
(currently read-only). Aggregate read paths already exist for laps/sessions (`/laps/aggregate`,
`/sessions/aggregate`, best-lap-per-track via `allTimeBest`).

This change makes the profile dynamic: backend generates the snapshot from local data, serves it,
and gates the feature behind a **runtime** toggle persisted to the config file.

## Goals / Non-Goals

**Goals:**
- Generate the `ProfileData` snapshot from local Session/Lap data + config identity.
- Serve it over HTTP, advertised via `_links`.
- Runtime on/off toggle (default on), persisted in the config file, exposed as a HATEOAS action.
- Gate the feature via `enabledFeatures`/`_links`; remove the frontend static-enable shortcut.

**Non-Goals:**
- The outbound cloud upload, pairing, payment ([add-cloud-publish] owns that). This change only
  *builds and serves* the snapshot locally; cloud-publish will send the same object.
- Per-user / multi-profile support, vanity-URL routing — single local user.
- Inbound auth on the snapshot endpoint (local install; same trust model as existing routes).
- Backfilling presentation-only fields that have no data source (track art, accent colours stay
  client-side defaults; estimated distance derived where feasible, otherwise omitted/approximated).

## Decisions

### D1: Runtime feature state via a resolver, not env-only
Add `PUBLIC_PROFILE` to `Feature`. Today `enabledFeatures()` is a pure env/default read. Introduce a
small `FeatureStateResolver` (or extend the index wiring) that, for `public-profile`, reads the
persisted toggle from `ApplicationConfiguration.publicProfile.enabled` instead of the env var; other
features keep env-var resolution. The `IndexController` is constructed per request scope today
(features captured at module init) — to reflect runtime changes, the enabled set + `_links` for the
profile must be computed **at request time** from the current config, not captured once at startup.
Alternative: env-var only (rejected — not a runtime toggle, contradicts the requirement).

### D2: Config file as the toggle + identity store
Extend `ApplicationConfiguration` with a `publicProfile: PublicProfileConfig` block:
`enabled: Boolean = false` plus identity (`name, slug, tagline, location, memberSince, season, sim`).
Add a **write** path to `JsonFileConfigurationRepository` (currently read-only) that re-serializes
the config file when the toggle flips. Persist via JSON5 to match the existing format. Hold the live
config in a single mutable holder injected where needed so reads see the latest value without a file
re-read per request. Alternative: DB settings table (rejected per user decision — config file).

### D3: Snapshot builder in the application layer
A `BuildProfileSnapshotUseCase` composes:
- **identity / meta** from config,
- **totals** (laps, sessions, active days, longest streak, tracks, cars, top car) and **perTrack**
  laps from existing aggregate read ports,
- **records** (season-best + all-time-best per track/car over valid laps) from the lap read port.
Output domain `ProfileSnapshot` → mapped to a `ProfileSnapshotResource` matching the frontend
`ProfileData` JSON. Reuse existing aggregate queries/ports where they fit; add a focused read port
only for gaps (e.g. best-per-track-car-with-date). Keeps domain inward-pointing per clean arch.

### D4: Two endpoints under one capability
- `GET /api/1/public-profile` → snapshot JSON (the data link, `rel: public-profile`).
- An action to flip the toggle — `PUT /api/1/public-profile/enabled` (idempotent set true/false),
  advertised as a distinct action rel (e.g. `publicProfileToggle`). Using PUT-set rather than a
  POST-toggle keeps it idempotent and lets the UI send the desired state explicitly.
Register in `App.kt` alongside the other controllers.

### D5: Frontend follows the link; sample becomes a MOCK fixture
`api/profile.ts`: `useProfileData()` becomes a TanStack Query keyed by `[publicProfile, mode, apiUrl]`
that, in LIVE, fetches `_links.public-profile`; in MOCK, returns the existing sample (now a test/mock
fixture, not the production source). Remove `STATICALLY_ENABLED` from `FeaturesProvider` and the fake
`rel` in `features.tsx`; the nav/route gate on `enabledFeatures` like every other feature. Add a
toggle control (calls the action link) and loading/error states to the screen.

## Risks / Trade-offs

- [Request-time feature resolution changes IndexController's startup-capture model] → Localize: only
  `public-profile` resolves dynamically; compute its link + flag in the route handler from the live
  config holder. Cover with a controller test toggling state between requests.
- [Config file writes can race / corrupt on concurrent toggles] → Serialize writes through the single
  config holder (one writer), write-then-rename for atomicity; toggles are rare and single-user.
- [Snapshot fields without a data source (track art, accents, exact distance)] → Keep presentation
  defaults client-side; derive distance only if track lengths are available, else approximate or omit
  — documented in `docs/public-profile.md`.
- [Divergence from add-cloud-publish's summary shape] → Treat this `ProfileSnapshot` as the canonical
  artifact; cloud-publish consumes it rather than defining its own, noted in both changes.
- [On-by-default exposes a profile built from default identity until the user fills it in] → Identity
  defaults are generic ("Driver"); the user edits the `publicProfile` config block, and can switch the
  profile off at runtime via the toggle. Docs/user-guide explain it.

## Migration Plan

Additive. No DB migration. On first run after upgrade the new `publicProfile` config block defaults
(enabled=false) apply; existing installs see the profile off until toggled on. Rollback = revert;
the config block is ignored by older builds. Mark the static-enable item DONE in
`docs/technical-debt.md`.

## Open Questions

- Exact `distanceKm` source — is per-track length data available locally? If not, approximate from
  lap count × nominal track length or omit. Resolve during implementation; not blocking.
- Should disabling the profile also stop serving `GET /api/1/public-profile` (404) or serve but hide
  in UI? Lean: omit the link + return 404 when disabled, consistent with the HATEOAS convention.
