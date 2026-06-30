# LapTime Insights — Frontend

React + Vite + TypeScript dashboard for the LapTime Insights API. Renders
sessions and laps from the Ktor backend, with a one-click toggle between
**mock** data (in-memory, deterministic, mirrors the backend seeder) and
**live** data (real HTTP / `/api/1/...` calls).

## Stack

- **Vite** — dev server + build (`pnpm dev` / `pnpm build`)
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

Requires **Node 24** (`.node-version` → `24.17.0`) and **pnpm**. `engines` + a `preinstall`
guard make `pnpm install` fail fast on an older Node — run `fnm use` first.

```bash
# Mock-only — no backend required.
cd frontend
fnm use                   # selects Node 24 from .node-version
pnpm install
pnpm dev                  # http://localhost:5173

# Live — requires backend running on :8000.
JDBC_URL=jdbc:h2:./data/laptime DB_SEED=true ./gradlew :app:run
# Then in the sidebar, toggle to LIVE.
```

## Type-check / build

```bash
pnpm typecheck
pnpm build
```

## Testing

```bash
pnpm test                          # Vitest unit / component tests (src/**)

pnpm exec playwright install chromium   # one-time browser download
pnpm e2e                           # Playwright + Serenity/JS E2E (e2e/**)
pnpm e2e:ui                        # interactive Playwright UI
pnpm e2e:report                    # open the last HTML report
```

E2E tests drive the real app in a browser using the **Serenity/JS Screenplay** pattern (Actors,
Tasks, Questions). They build + serve the production bundle (`vite preview`, port 4173) and run in
**MOCK** data mode, so no backend is needed. Details and the directory layout are in
[`docs/frontend-technical.md`](../docs/frontend-technical.md) § Testing.

## Feedback form

A **Feedback** launcher in the Topbar lets users file a bug / suggestion / general
feedback. It submits straight to a **Google Form** (`no-cors` POST to `…/formResponse`) —
no backend — and the form's linked Google Sheet is the inbox. See
[../docs/frontend-technical.md](../docs/frontend-technical.md#feedback-form) for the
architecture; this section is the **operator setup**.

> ⚠️ **Build-time config.** `VITE_*` vars are inlined by `vite build`, **not** read at
> runtime. A downloader of the self-hosted product cannot configure this — whoever runs the
> build bakes in the form. To make feedback reach you, set the vars before the build that you
> distribute. The launcher is hidden when `VITE_FEEDBACK_FORM_URL` is empty.

**Setup:**

1. Create a Google Form with at least a **type** question (e.g. multiple choice:
   Bug / Suggestion / General feedback) and a **message** question (paragraph). Optionally add
   short-answer questions for email, app version, and screen.
2. Get the field ids: open the form's **pre-filled link** (⋮ → *Get pre-filled link*), fill
   dummy answers, copy the link, and read the `entry.<id>` for each question. The form action
   is the same URL with `/viewform` replaced by `/formResponse`.
3. Put the values in **`.env.production`** (committed — loaded automatically by `pnpm build`):

   ```bash
   VITE_FEEDBACK_FORM_URL=https://docs.google.com/forms/d/e/<form-id>/formResponse
   VITE_FEEDBACK_ENTRY_TYPE=entry.<id>
   VITE_FEEDBACK_ENTRY_MESSAGE=entry.<id>
   VITE_FEEDBACK_ENTRY_EMAIL=entry.<id>     # optional
   VITE_FEEDBACK_ENTRY_VERSION=entry.<id>   # optional
   VITE_FEEDBACK_ENTRY_SCREEN=entry.<id>    # optional
   ```

   A `/formResponse` URL is a public endpoint, not a secret, so committing it is fine. For
   local testing against a throwaway form, use `.env.local` (git-ignored) instead. See
   `.env.example` for the full annotated list.
4. `pnpm build` and ship `dist/`. In **MOCK** data mode no request is sent (simulated
   success), so the form is safe to click through locally.

Notes:
- Optional `entry.*` ids that are omitted simply drop that field from the submission.
- A `no-cors` response is opaque, so a request that doesn't throw is treated as success — the
  Google Sheet is the source of truth, not the in-app confirmation.
- The attached **app version** is `frontend/package.json`'s `version` (injected as
  `__APP_VERSION__`), so bump it per release to keep feedback diagnosable.

## Lap comparison

The `/compare` screen overlays two laps' telemetry traces against
`splinePosition`.

### Entry points

Picking laps from a long list is the failure mode, not the happy path. The
common ways into compare are:

- **From SessionDetail row**: each lap row exposes `vs best` (against the
  fastest valid lap of the same session) and `vs PB` (against the all-time
  fastest lap at that track). One click, no picking. Disabled when the
  current row is itself the relevant best.
- **From the Laps screen**: "Select to compare" enters multi-select mode;
  tick two rows and click "Compare selected". The user picks the filters
  (track/car/PB/page) before tapping the rows, so this works at scale.
- **Direct on /compare**: the `LapPicker` is a button that opens a modal
  containing a paginated, filterable lap browser. URL state owns `lap1`,
  `lap2` and an optional `track` hint, so a chosen comparison is shareable
  via copy-paste.

### Implementation

- `useLapComparison(lap1Uid, lap2Uid)` hits `GET /api/1/laps/compare`. The
  backend returns raw samples for both laps in one round trip.
- `useSessionBestLap(sessionUid)` and `useTrackBestLap(track)` are thin
  wrappers around `GET /api/1/laps?…&validLap=true&sort=lapTime:ASC&size=1`.
- `LapBrowser` is the reusable filtered + paginated lap list used both inside
  the `LapPicker` modal and (in spirit) on the Laps screen. Toggles for
  valid-only and PB-only mirror the screen-level toggles.
- Charts: `TelemetryTrace` overlays multiple series; `SpeedDeltaTrace`
  resamples both laps to 100 buckets and plots the per-bucket KPH delta;
  `GearMismatchStrip` highlights buckets where the two laps used different
  gears.

## URL state

Filter and pagination state for screens with searchable lists is mirrored to
the URL querystring. Reload-safe and shareable: `/laps?track=Monza&pb=true&page=2`
restores the same view. Implemented in `src/hooks/useUrlState.ts`:

```ts
const [params, setParam, setMany] = useUrlState();
const track = getString(params, "track");
const page  = getInt(params, "page", 1);
setParam("track", "Monza");        // → ?track=Monza
setMany({ track: undefined, page: undefined });  // strip both
```

When adding a new screen with filters, **always** drive state from
`useUrlState` rather than `useState` so deep-linking keeps working.

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
