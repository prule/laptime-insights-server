# Public Profile (Driver Passport)

A subscriber-facing, shareable "vanity" page that shows off a season of ACC driving — the
**Driver Passport** concept from the design handoff. Reachable from the left-hand sidebar
(**Public Profile** → `/profile`).

## Status

- **Backend-generated and HATEOAS-gated.** The page fetches its snapshot from
  `GET /api/1/public-profile`, which the backend generates from the local Session/Lap data merged
  with signup identity (`frontend/src/api/profile.ts` → `useProfileData()` follows the
  `public-profile` index link). In MOCK mode the bundled `SAMPLE_PROFILE` is served by the mock
  handler; `SAMPLE_PROFILE` is otherwise only a test fixture.
- **Runtime on/off toggle.** On by default. The state lives in the app config file
  (`publicProfile.enabled` in `ApplicationConfiguration`) and is flipped via
  `PUT /api/1/public-profile/enabled`, advertised as the `publicProfileToggle` action link and
  surfaced as a control in the sidebar footer (LIVE mode). When off, the `public-profile` capability
  link + `enabledFeatures` flag are absent and `GET /api/1/public-profile` returns `404`.
- Next phase (`add-cloud-publish`): a paying user's install uploads **this same snapshot** to the
  cloud, which hosts it at a vanity URL. Cloud-publish should consume the snapshot built here rather
  than defining its own.

## Backend

- Identity + toggle: `ApplicationConfiguration.publicProfile` (`PublicProfileConfig`) — `enabled`
  plus `name, slug, tagline, location, memberSince, season, sim`. Identity feeds the snapshot parts
  that telemetry can't supply; toggle writes are persisted by `ConfigurationStore` /
  `JsonFileConfigurationRepository.saveConfiguration` (atomic temp-file + rename).
- Generation: `BuildProfileSnapshotService` composes identity with aggregates from
  `ProfilePersistenceAdapter` (player laps only): totals, per-track laps, active days (streak +
  heatmap source), and per-(track, car) season-vs-all-time bests over valid laps. The
  `public-profile` feature's enabled state is resolved **per request** in `IndexController` from the
  live config (all other features stay env-var-resolved at startup).
- **`distanceKm` is an estimate** — local data has no per-track length, so it is approximated as
  `laps × BuildProfileSnapshotService.NOMINAL_LAP_KM` (5 km). Replace with real track lengths if/when
  available.

## Data contract — `ProfileData`

Defined and documented in `frontend/src/api/profile.ts`; the backend `ProfileSnapshotResource`
mirrors it field for field (`member_since` is snake_case on the wire). This is the **contract of
record** — the same JSON the page renders and the cloud upload will send. One season's aggregates +
all-time bests:

| Field       | Purpose |
|-------------|---------|
| `meta`      | slug, season label, date range, `generatedAt`, sim |
| `profile`   | identity (name, slug, initials, tagline, location, member_since) |
| `totals`    | season totals (laps, distanceKm, hours, sessions, daysActive, longestStreak, tracks, cars, topCar) |
| `perTrack`  | laps per circuit (+ accent colour, track-art key) |
| `records`   | per track/car season-best vs all-time-best (+ `isPB`) |
| `highlight` | optional headline stat for the share card (omitted by the backend snapshot) |

Helpers in the same module: `TRACK_ART` (circuit outline SVG paths) and `buildHeatmap()`
(client-side GitHub-style activity grid for the demo; real per-day lap data drives the live page).

## Components

`screens/PublicProfileScreen.tsx` composes prop-driven presentational components under
`components/profile/`: `LicenseCard`, `SeasonTotals`, `ActivityHeatmap`, `TrackStamps`,
`RecordsLedger`. Styling uses the app's Tailwind tokens; only decorative passport chrome (foil
strip, guilloché pattern, gold chip, per-track accents) stays as inline/arbitrary styles.
