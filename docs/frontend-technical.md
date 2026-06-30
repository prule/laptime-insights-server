# Frontend Technical Documentation

React + Vite + TypeScript dashboard for the LapTime Insights API. Targets the Ktor backend's REST/HATEOAS API on port 8000. Supports a one-click toggle between **mock** (in-memory, no server needed) and **live** (real HTTP) data modes.

## Stack

| Concern | Library |
|---------|---------|
| Build | Vite |
| Language | TypeScript (strict) |
| Styling | Tailwind CSS — design tokens in `tailwind.config.ts` |
| Data fetching | TanStack Query (React Query) |
| Routing | React Router v6 |

## Directory layout

```
frontend/src/
  api/
    types.ts          # TS shapes mirroring Ktor JSON resources
    client.ts         # fetch wrapper, HATEOAS link helper, mock/live dispatch
    queries.ts        # all TanStack Query hooks
    mock/
      data.ts         # deterministic dataset mirroring DatabaseSeeder
      handler.ts      # in-memory router matching Ktor paths + query params
  components/
    layout/           # AppShell, Sidebar, Topbar, TimeRangeSelector
    ui/               # Card, Badge, Delta, StatCard, BarChart, Sparkline,
                      # TelemetryTrace, SpeedDeltaTrace, GearTrace, TrackMap,
                      # FilterSelect, Modal, SectionHeader, States, TrackPracticeChart
    LapLeaderboard.tsx # ranked same-track lap list (challenger picker + anchor-change modal)
    AnchorControl.tsx  # anchor lap display + change-modal (Compare screen)
    SessionRow.tsx    # single session row (Overview + Sessions screens)
  config/
    features.tsx      # FEATURE_CONFIG registry: rel ↔ nav ↔ routes for every feature
  hooks/
    useUrlState.ts    # URL querystring read/write helpers
    useCompareSeed.ts # latest-session seed + default anchor for the Compare screen
  lib/
    format.ts         # formatLapTime / formatDate / formatDrivingTime / formatNumber
  providers/
    DataModeProvider.tsx   # mock|live toggle + apiBase, persisted to localStorage
    FeaturesProvider.tsx   # fetches GET /api/1; exposes useFeatureEnabled(feature)
    TimeRangeProvider.tsx  # global time range (1m/3m/6m/1y/all), persisted to localStorage
  screens/
    OverviewScreen.tsx
    SessionsScreen.tsx
    SessionDetailScreen.tsx
    LapsScreen.tsx
    CompareScreen.tsx
  App.tsx             # route definitions
  main.tsx            # bootstrap (QueryClient, Router, providers)
```

## Screens

### OverviewScreen (`/`)

Fetches sessions (`size: 100, sort: startedAt:DESC, from: <timeRange>`) and laps (`size: 1000, sort: lapTime:ASC, from: <timeRange>`), plus the full session options list (unrestricted, for track universe).

Key computed values:

- `stats` — totalSessions, totalLaps, bestLap (fastest valid lap), avgLap (mean valid lap time).
- `lapBuckets` / `sessionBuckets` — passed to `BarChart` via `groupByPlan()`, which partitions timestamps into weekly or monthly buckets ending at `now` (finite ranges) or at the data maximum (`all`).
- `trackPractice` — joins laps to sessions via a `Map<sessionUid, track>`, then aggregates lap counts per track. Tracks present in session options but absent from in-range sessions appear as zero-count entries so the bubble chart shows "haven't been here lately" placeholders.

**Known limit:** `lapsQuery` is capped at 1000 items. Track counts under-count if more than 1000 laps fall in the active range. A dedicated `/laps/aggregate?groupBy=track` endpoint would fix this cleanly.

### SessionsScreen (`/sessions`)

Passes `track`, `car`, `simulator`, `from`, `size: 50`, `sort: startedAt:DESC` to `useSessions`. Filters live in the URL (`useUrlState`). Clearing resets all three params simultaneously using `setMany`.

### SessionDetailScreen (`/sessions/:uid`)

Loads:
- `useSession(uid)` — session metadata.
- `useSessionLaps(uid)` — all laps in that session (unfiltered).
- `useSessionBestLap(uid)` — `GET /api/1/laps?sessionUid=…&validLap=true&sort=lapTime:ASC&size=1`.
- `useTrackBestLap(track)` — `GET /api/1/laps?track=…&validLap=true&sort=lapTime:ASC&size=1`.

The per-row compare buttons navigate to `/compare?track=…&anchor=…&challenger=…` (anchor = the row's lap) so the comparison arrives pre-filled. The "pick…" button omits `challenger` and lands on the leaderboard.

### LapsScreen (`/laps`)

URL params: `track`, `car`, `simulator`, `invalid` (bool, inverted to `validOnly`), `pb` (bool), `player` (bool — restricts to laps recorded by the player's car via `playerLap=true`), `page` (int, default 1), `sort` (string, `field:ASC|DESC`, default `lapTime:ASC`). Page size is 50.

Column headers are sortable via the `sortable: string[]` field returned by the backend on `Page<LapResource>` — `LapTable` only renders a clickable header for fields that appear in that list, so adding/removing sortable columns on the server doesn't need a frontend change. Clicking a header cycles ASC → DESC → cleared (reverting to the default). State is mirrored to the `sort` URL param.

Multi-select mode is local component state — selection is transient, not URL-serialised. Selecting a second lap when two are already chosen drops the oldest pick so the most recent two win. Navigates to `/compare` with `anchor` and `challenger` set, plus `track` (the active facet, or derived from the anchor lap) so the Compare axis is always present.

### CompareScreen (`/compare`)

URL params: `track` (the shared comparison axis), `anchor`, `challenger` (lap uids). Old `lap1`/`lap2` links are still honored — they are read as `anchor`/`challenger`.

Track is the comparison axis: laps can only be compared within one track. On a fresh landing (`track`/`anchor` absent), `useCompareSeed(track)` seeds the screen from the latest session — `useSessions({ sort: "startedAt:DESC", size: 1 })` for the track + car, and the player's fastest valid lap in that session (via `useSessionLaps`, falling back to the session best, then `useTrackBestLap`) as the default anchor. An explicit `track`/`anchor` param always wins, so the seed never overrides shared links or the "vs best"/"vs PB" entry points.

- **`AnchorControl`** renders the anchor and opens a `LapLeaderboard` modal to change it.
- **`LapLeaderboard`** is the challenger picker: a ranked (`lapTime:ASC`) same-track list with toggles for scope (`This session` via `useSessionLaps`, client-filtered; `All sessions` via server-paginated `useLaps`), driver (`Me` → `playerLap=true`), and `Same car` (default ON → `car=<anchor car>`). Rank is the absolute position in the filtered list; the "me" dot and owning session come from `LapTable`. The anchor row is marked and disabled as a challenger pick.

Calls `useLapComparison(anchorUid, challengerUid)` which hits `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…`. The backend returns raw telemetry samples for both laps in one round trip.

`hoveredPosition: number | null` is lifted to `CompareScreen` and passed as props to every chart and the track map. Any chart that the user hovers emits `onHover(position)`, which sets the shared state and synchronizes all other panels.

Charts and panels:
- `TelemetryTrace` — generic multi-series SVG trace; `series` prop is `{ samples, color, label }[]`. Renders speed overlaid for both laps. Accepts `hoveredPosition` / `onHover` for the synchronized crosshair; shows a tooltip with per-lap values at the cursor position.
- `SpeedDeltaTrace` — resamples both lap sample arrays to 100 equidistant buckets by `splinePosition`, then plots Lap 1 KPH minus Lap 2 KPH per bucket. Supports crosshair; tooltip shows signed delta at cursor.
- `GearTrace` — both laps' gear as overlaid **stepped** lines (discrete integers) over an integer Y axis, plotted against track position. Track positions where the gears differ (100-bucket resample) are shaded behind the traces, preserving the old mismatch insight. Supports the shared crosshair.
- `TrackMap` — renders the track outline as an SVG polyline from `worldPosX`/`worldPosY` coordinates normalised into a square viewBox. A coloured dot per lap moves to the nearest sample when `hoveredPosition` changes. Hovering the map emits the nearest sample's `splinePosition` so all charts follow. Start/finish line is marked with a white rectangle.

## Providers

### DataModeProvider

Wraps the app. Provides `mode` (`"mock" | "live"`) and `apiBase` (defaults to `""` for Vite proxy; can be overridden via `localStorage.setItem("lti.apiUrl", "https://…")`). Persisted under `lti.dataMode`.

All TanStack Query keys include `[mode, apiBase]` so toggling modes triggers a full refetch and never serves cross-mode stale data.

### TimeRangeProvider

Provides `range` (`"1m" | "3m" | "6m" | "1y" | "all"`), `fromIso` (inclusive ISO-8601 lower bound or `null` for `all`), and `bucketPlan` (`{ count, unit }`). Persisted under `lti.timeRange`. Default: `1m`.

Bucket plans:

| Range | Unit | Count |
|-------|------|-------|
| 1m | week | 4 |
| 3m | week | 12 |
| 6m | month | 6 |
| 1y | month | 12 |
| all | month | 24 |

`fromIso` is recomputed on selection change only (not on every render), so a long session does not silently drift the query boundary.

## URL state

`useUrlState` wraps `useSearchParams` and exposes `[params, setParam, setMany]`. Helpers:

```ts
getString(params, "track")          // string | undefined
getBool(params, "validOnly", false) // boolean
getInt(params, "page", 1)           // number

setParam("track", "Monza")          // → ?track=Monza
setParam("page", undefined)         // removes the key
setMany({ track: undefined, page: undefined }) // atomic multi-key update
```

**Rule:** all filter and pagination state on list screens must go through `useUrlState`, not `useState`. This keeps deep links and browser back/forward working.

Multi-select state on the Laps screen is an exception — selection is transient pre-action state, not a shareable view.

## Data fetching

All hooks live in `src/api/queries.ts`. Each wraps `apiGet` (from `client.ts`) and includes `[mode, apiBase]` in its query key.

Key hooks:

| Hook | Backend route |
|------|--------------|
| `useSessionOptions()` | `GET /api/1/sessions/options` |
| `useSessions(filters)` | `GET /api/1/sessions` |
| `useSession(uid)` | `GET /api/1/sessions/{uid}` |
| `useSessionLaps(uid)` | `GET /api/1/sessions/{uid}/laps` |
| `useSessionBestLap(uid)` | `GET /api/1/laps?sessionUid=…&validLap=true&sort=lapTime:ASC&size=1` |
| `useTrackBestLap(track)` | `GET /api/1/laps?track=…&validLap=true&sort=lapTime:ASC&size=1` |
| `useLaps(filters)` | `GET /api/1/laps` |
| `useLapComparison(uid1, uid2)` | `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…` |

`useLapComparison` is disabled when either uid is `undefined`.

## Mock layer

`src/api/mock/handler.ts` is an in-memory router. It matches request URLs against patterns that mirror the Ktor routes, applies the same filter/sort/pagination logic as the backend, and returns JSON shaped identically to the real API (including HATEOAS `_links`). The dataset in `src/api/mock/data.ts` mirrors the backend's `DatabaseSeeder` profiles.

When adding a new backend endpoint:
1. Add the route handler to `mock/handler.ts`.
2. Add sample data to `mock/data.ts` if needed.
3. Add the query hook to `queries.ts`.
4. Use the hook from a screen.

## HATEOAS

`SessionResource` and `LapResource` include `_links: Record<string, string>`. `SessionRow` navigates via `session._links.self` rather than constructing `/sessions/{uid}` itself. `client.ts` exposes `fetchLink(ctx, links, rel)` for following arbitrary link relations. If the backend changes its URL scheme, the frontend follows without code changes.

## Feedback form

A **Feedback** launcher sits in the Topbar (beside the time-range selector) on every
screen. It opens a modal capturing a **type** (bug / suggestion / general feedback), a
required **message**, and an optional **email**, and auto-attaches the **app version**
(`__APP_VERSION__`, injected from `package.json` via Vite `define`) and the **current
screen** (`useLocation().pathname`).

Submission goes straight to a **Google Form** via a `no-cors` POST to its `formResponse`
endpoint — the same technique the landing page's register-interest form uses. There is no
backend involvement; the form's linked Google Sheet is the inbox. A `no-cors` response is
opaque, so a fetch that resolves without throwing is treated as success. In MOCK data mode
no request is sent — submission is simulated so the UI works offline.

Code: `src/components/feedback/FeedbackButton.tsx` (launcher + modal),
`src/api/feedback.ts` (payload, validation, submit), `src/config/feedback.ts` (config),
`src/lib/version.ts` (`APP_VERSION`).

**Configuration** is presence-gated, not a HATEOAS/`enabledFeatures` toggle: the launcher
renders only when a form is configured.

| Env var | Required | Maps to |
|---------|----------|---------|
| `VITE_FEEDBACK_FORM_URL` | yes | Google Form `…/formResponse` action |
| `VITE_FEEDBACK_ENTRY_TYPE` | yes | `entry.<id>` for the type question |
| `VITE_FEEDBACK_ENTRY_MESSAGE` | yes | `entry.<id>` for the message question |
| `VITE_FEEDBACK_ENTRY_EMAIL` | no | `entry.<id>` for the email question |
| `VITE_FEEDBACK_ENTRY_VERSION` | no | `entry.<id>` for the app version |
| `VITE_FEEDBACK_ENTRY_SCREEN` | no | `entry.<id>` for the current screen |

Optional entry ids that are omitted simply drop that field from the submission.

### Build-time, not runtime — and which file to use

`VITE_*` vars are **inlined into the JS bundle when `pnpm build` runs**; nothing reads them
at runtime, so a downloader of the self-hosted product cannot set them. Whoever produces the
distributed build determines the baked-in form. Two files matter:

- **`frontend/.env.production`** — committed on purpose, loaded automatically by
  `vite build`. Put the maintainer's real form URL + entry ids here so the launcher reaches
  *your* Google Form in **every** production build (CI, release job, or a build from source).
  A `/formResponse` URL is a public endpoint, not a secret. Leave blank to ship the launcher
  hidden. (Both `.env.example` and `.env.production` have explicit `!**/...` exceptions in the
  root `.gitignore`, since `.env*` is otherwise ignored.)
- **`frontend/.env.local`** — git-ignored per-developer override, loaded in `pnpm dev` and
  local builds. Use it to point at a test form without touching the committed config.

If you'd rather keep the URL out of git entirely, set the same vars as CI/release secrets in
the build step instead of committing `.env.production` — but then builds from source won't
enable feedback.

## Running

**Toolchain:** Node 24 (pinned via `.node-version` → `24.17.0`) and pnpm. `package.json`
declares `engines.node >= 24` and runs a `preinstall` guard, so `pnpm install` fails
fast on an older Node — run `fnm use` first.

```bash
# Mock only — no backend required
cd frontend && fnm use && pnpm install && pnpm dev   # http://localhost:5173

# Live — backend must be running on :8000
JDBC_URL=jdbc:h2:./data/laptime DB_SEED=true ./gradlew :app:run
# Then toggle to LIVE in the sidebar
```

Vite proxies `/api/...` → `http://localhost:8000` in dev. For a different host, set `lti.apiUrl` in `localStorage`.

## Type-check and build

```bash
cd frontend
pnpm typecheck
pnpm build
```

## Testing

Two independent layers:

- **Unit / component (Vitest)** — `pnpm test`. Pure-function tests are `*.test.ts` (node env);
  component/hook tests are `*.test.tsx` and opt into jsdom per-file via a
  `// @vitest-environment jsdom` pragma. Vitest only globs `src/**`.
- **End-to-end (Playwright + Serenity/JS Screenplay)** — `pnpm e2e`. Browser-level tests live in
  `e2e/` (outside `src/`, so they never overlap Vitest or the `tsc` build).

```bash
cd frontend
pnpm exec playwright install chromium   # one-time: download the browser
pnpm e2e                                # build + preview + run the suite
pnpm e2e:ui                             # interactive Playwright UI
pnpm e2e:report                         # open the last HTML report
```

**Why MOCK mode.** `DataModeProvider` defaults to MOCK and only switches to LIVE from
`localStorage["lti.dataMode"]`. A fresh Playwright browser context has empty localStorage, so the
suite runs entirely against the in-memory mock — deterministic, no backend, no ACC telemetry. The
Playwright `webServer` runs `pnpm build && pnpm preview` (port 4173) so tests hit the production
bundle.

**Screenplay layout.** Tests are written as an Actor (`Driver`) performing Tasks and asking
Questions — no raw Playwright locators in spec bodies.
- `e2e/screenplay/screens.ts` — screen registry + `PageElement`/`PageElements` targets.
- `e2e/screenplay/tasks.ts` — reusable Tasks (`OpenTheApp`, `NavigateToScreen`,
  `SelectAllTimeRange`, `OpenTheFirstSession`).
- `e2e/screenplay/questions.ts` — Questions (`TheScreenTitle`, `TheNumberOfSessions`, `TheNumberOfLaps`).
- `e2e/specs/` — the tests themselves.

Selectors are role/heading-first; a small set of `data-testid` hooks cover ambiguous nodes
(`screen-*` roots, `screen-title`, `session-row`, `laps-table`, `lap-row`, `time-range-*`).
Note the default time range is 1 month, and seeded mock sessions predate that — use the
`SelectAllTimeRange` Task before asserting on list rows.

CI runs the suite as a dedicated `e2e` job (see `.github/workflows/build-and-test.yml`); a failing
test blocks the check, and reports/traces upload as artifacts on failure.

## Adding a new screen

1. Create `src/screens/NewScreen.tsx`.
2. Pick an existing feature in `src/config/features.tsx` (or add a new one — see below) and append
   the screen's route to that feature's `routes` array. `App.tsx` and `Sidebar` derive from this
   registry, so no further wiring is needed there.
3. Drive any filter/pagination state through `useUrlState`.
4. Add corresponding mock handler and query hook (call `useGate(feature, …)` inside the hook so
   it short-circuits when the feature is off).
5. Use `LoadingState`, `ErrorState`, `EmptyState` from `components/ui/States.tsx` for async states.

## Adding a new feature

A feature is the unit the backend toggles via `FEATURE_<NAME>` env vars and the frontend reads
from `GET /api/1` `_links`.

1. Add the enum entry to `app/.../Feature.kt` and emit its link in `IndexLinkFactory`.
2. Add the matching string to the `Feature` union in `src/config/features.tsx` and append a
   `FEATURE_CONFIG` entry with the rel, sidebar nav, and routes.
3. Update the mock handler (`api/mock/handler.ts`) to advertise the rel in its `/api/1` response.
4. Reference the feature via `useFeatureEnabled("yourFeature")` in any cross-screen action button
   so it hides when the feature is disabled.
