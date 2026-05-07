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
                      # TelemetryTrace, SpeedDeltaTrace, GearMismatchStrip, TrackMap,
                      # FilterSelect, Modal, SectionHeader, States, TrackPracticeChart
    LapBrowser.tsx    # reusable filtered+paginated lap list (used by LapPicker modal)
    LapPicker.tsx     # button + Modal wrapping LapBrowser for lap selection
    SessionRow.tsx    # single session row (Overview + Sessions screens)
  config/
    navigation.ts     # NAV_ITEMS array driving the sidebar
  hooks/
    useUrlState.ts    # URL querystring read/write helpers
  lib/
    format.ts         # formatLapTime / formatDate / formatDrivingTime / formatNumber
  providers/
    DataModeProvider.tsx   # mock|live toggle + apiBase, persisted to localStorage
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

The per-row compare buttons navigate to `/compare?track=…&lap1=…&lap2=…` so both pickers arrive pre-filled.

### LapsScreen (`/laps`)

URL params: `track`, `car`, `simulator`, `invalid` (bool, inverted to `validOnly`), `pb` (bool), `page` (int, default 1). Page size is 50.

Multi-select mode is local component state — selection is transient, not URL-serialised. Selecting a second lap when two are already chosen drops the oldest pick so the most recent two win. Navigates to `/compare` with `lap1` and `lap2` set, plus `track` if a track filter is active.

### CompareScreen (`/compare`)

URL params: `lap1`, `lap2`, `track` (optional hint pre-filling `LapPicker` modals).

Calls `useLapComparison(lap1Uid, lap2Uid)` which hits `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…`. The backend returns raw telemetry samples for both laps in one round trip.

`hoveredPosition: number | null` is lifted to `CompareScreen` and passed as props to every chart and the track map. Any chart that the user hovers emits `onHover(position)`, which sets the shared state and synchronizes all other panels.

Charts and panels:
- `TelemetryTrace` — generic multi-series SVG trace; `series` prop is `{ samples, color, label }[]`. Renders speed overlaid for both laps. Accepts `hoveredPosition` / `onHover` for the synchronized crosshair; shows a tooltip with per-lap values at the cursor position.
- `SpeedDeltaTrace` — resamples both lap sample arrays to 100 equidistant buckets by `splinePosition`, then plots Lap 1 KPH minus Lap 2 KPH per bucket. Supports crosshair; tooltip shows signed delta at cursor.
- `GearMismatchStrip` — same 100-bucket resampling; renders a coloured strip where buckets with differing gear values are highlighted. Supports crosshair.
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

## Running

```bash
# Mock only — no backend required
cd frontend && npm install && npm run dev   # http://localhost:5173

# Live — backend must be running on :8000
JDBC_URL=jdbc:h2:./data/laptime DB_SEED=true ./gradlew :app:run
# Then toggle to LIVE in the sidebar
```

Vite proxies `/api/...` → `http://localhost:8000` in dev. For a different host, set `lti.apiUrl` in `localStorage`.

## Type-check and build

```bash
cd frontend
npm run typecheck
npm run build
```

## Adding a new screen

1. Create `src/screens/NewScreen.tsx`.
2. Add a route in `App.tsx`.
3. Add an entry to `NAV_ITEMS` in `src/config/navigation.ts`.
4. Drive any filter/pagination state through `useUrlState`.
5. Add corresponding mock handler and query hook.
6. Use `LoadingState`, `ErrorState`, `EmptyState` from `components/ui/States.tsx` for async states.
