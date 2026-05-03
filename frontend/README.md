# LapTime Insights — Frontend

React + Vite + TypeScript dashboard for the LapTime Insights API. Renders
sessions and laps from the Ktor backend, with a one-click toggle between
**mock** data (in-memory, deterministic, mirrors the backend seeder) and
**live** data (real HTTP / `/api/1/...` calls).

## Stack

- **Vite** — dev server + build (`npm run dev` / `npm run build`)
- **TypeScript** — strict mode, path alias `@/* → src/*`
- **Tailwind CSS** — design tokens defined in `tailwind.config.ts` and used directly via utility classes
- **TanStack Query** — request caching, retry, dedupe; queryKeys include the data mode so flipping modes invalidates cleanly
- **React Router** — file-system style routing (`/`, `/sessions`, `/sessions/:uid`, `/laps`)

## Layout

```
src/
  api/
    types.ts          # TS shapes mirroring the Ktor JSON (SessionResource, LapResource, Page<T>)
    client.ts         # fetch wrapper + HATEOAS link helper + mock/live dispatch
    queries.ts        # useSessionOptions / useSessions / useSession / useSessionLaps / useLaps
    mock/
      data.ts         # mirrors DatabaseSeeder profiles
      handler.ts      # in-memory router that matches Ktor paths + filters
  components/
    layout/           # AppShell, Sidebar, Topbar
    ui/               # Card, Badge, Delta, StatCard, BarChart, Sparkline, States, SectionHeader
    SessionRow.tsx    # uses HATEOAS `_links.self` for navigation
  config/             # nav definitions
  lib/format.ts       # lap-time / date / duration formatting
  providers/          # DataModeProvider (mock|live + apiUrl, persisted in localStorage)
  screens/            # OverviewScreen, SessionsScreen, SessionDetailScreen, LapsScreen
  App.tsx             # routes
  main.tsx            # bootstrap
```

## Data modes

The bottom-left button in the sidebar toggles between **MOCK** and **LIVE**:

| Mode | Behavior |
|------|----------|
| `mock` (default) | API calls are routed to `src/api/mock/handler.ts`. The dataset in `mock/data.ts` matches the shape produced by `app/.../adapter/in/web/.../*Resource.kt` and the values match `DatabaseSeeder` profiles. |
| `live`           | Calls hit the backend. With the Vite dev server, requests to `/api/...` are proxied to `http://localhost:8000` (see `vite.config.ts`). For other hosts, set the API URL via `localStorage.setItem("lti.apiUrl", "https://...")`. |

The data mode is part of every TanStack Query key (`["sessions", mode, apiBase, filters]`),
so toggling re-fetches and never serves stale cross-mode data.

## HATEOAS

`SessionResource` and `LapResource` both expose a `_links` map with at least
`self`. `SessionRow` navigates by reading `session._links.self`, so if the
backend changes its URL scheme (e.g. moves `/api/1/sessions/{uid}` to a new
base path), the frontend follows without code changes. The fetch wrapper
also exposes `fetchLink(ctx, links, rel)` for following arbitrary relations.

## Running

```bash
# Mock-only — no backend required.
cd frontend
npm install
npm run dev               # http://localhost:5173

# Live — requires backend running on :8000.
JDBC_URL=jdbc:h2:./data/laptime DB_SEED=true ./gradlew :app:run
# Then in the sidebar, toggle to LIVE.
```

## Type-check / build

```bash
npm run typecheck
npm run build
```

## Adding a new endpoint

1. Add a handler to `src/api/mock/handler.ts` that mirrors the Ktor route.
2. Add a hook to `src/api/queries.ts` that wraps `apiGet` and includes the
   data mode in its `queryKey`.
3. Use the hook from a screen. Loading / error / empty states live in
   `components/ui/States.tsx` so screens stay short.

## Where the legacy prototype lives

The original `Dashboard.html` plus the standalone JSX files now live in
`_legacy/` for reference. They're not loaded by the new app and can be
deleted once the rewrite is fully validated.
