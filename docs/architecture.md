This architecture document outlines the technical blueprint for the **LapTimeInsights** ecosystem. It adheres to **Clean
Architecture** principles to ensure the business logic (telemetry analysis) remains decoupled from the infrastructure (
Ktor, Exposed, React).

---

## 1. System Overview

LapTimeInsights consists of a **Kotlin/Ktor** server acting as a local hub and a **React/TypeScript** dashboard.

* **Deployment model:** LapTimeInsights is **self-hosted**. The user downloads it and runs it locally on their own
  network, on the **same network as their ACC server**. There is no cloud/SaaS component; all data stays on the user's
  network. The dashboard is accessed via a browser on that PC or another device on the same LAN.
* **Backend:** Handles telemetry ingestion via `acc-client`, persists data using **Exposed**, and serves a
  **REST/HATEOAS** API.
* **Frontend:** A reactive dashboard built with **React** + **TanStack Query** for data fetching, consuming the HATEOAS
  API to drive navigation.

---

## 2. Backend Architecture: Clean & Hexagonal

The server is split into four distinct layers. Dependencies point **inwards** only.

### I. Domain Layer (The Core)

* **Entities:** Pure Kotlin data classes (e.g., `Lap`, `Session`, `RealtimeCarUpdate`, `TelemetrySample`).
* **Ports:** Boundary definitions expressed as interfaces (e.g., `CreateLapPort`, `FindRealtimeCarUpdateByLapPort`).
* **Business Logic:** Logic that doesn't belong in a use case, such as calculating "Effort Score" or "Grip Multipliers."

### II. Use Case Layer (Application)

* **Interactors:** Orchestrate the flow of data (e.g., `CreateLapUseCase`, `CompareLapsUseCase`).
* **Ports:** Input ports (Use Case interfaces) and Output ports (persistence / event interfaces).

### III. Interface Adapters (The Glue)

* **Controllers/Routes:** Ktor routing definitions.
* **HATEOAS Presenters:** Converts Domain Entities into **HAL (Hypertext Application Language)** JSON.
    * *Example:* A Lap resource includes a `_links` object with a URI to its `telemetry` or its parent `session`.
* **Repositories Implementation:** **Exposed** DSL logic that interacts with the database.

### IV. Infrastructure Layer

* **Ktor Server:** Configuration, Plugins (ContentNegotiation, CORS, OpenAPI).
* **Telemetry Client:** Integration with `acc-client` to bridge external ACC UDP broadcast data into the app via
  `ClientInitializer`. Handles `RealtimeUpdate`, `RealtimeCarUpdate`, `LapCompleted`, `EntryListCar`, and `TrackData`
  message types.

---

## 3. Backend Technology Stack

| Component                | Technology                                 |
|:-------------------------|:-------------------------------------------|
| **Language**             | Kotlin 2.x                                 |
| **Web Framework**        | Ktor (Server-side)                         |
| **ORM / Database**       | JetBrains Exposed v1 / H2 (local dev)      |
| **API Style**            | REST with HATEOAS (HAL+JSON)               |
| **Dependency Injection** | Manual wiring in `AppModule.kt` (no Koin)  |
| **Migrations**           | Flyway                                     |
| **Serialization**        | kotlinx.serialization                      |

---

## 4. Frontend Architecture

The frontend is a single-page React application built with Vite.

### Directory Structure (`src/`)

* **`api/`** — TypeScript resource types (`types.ts`), fetch wrapper + HATEOAS helpers (`client.ts`), all TanStack
  Query hooks (`queries.ts`), and an in-memory mock layer (`mock/`).
* **`components/`** — Shared UI components:
    * `layout/` — AppShell, Sidebar, Topbar, TimeRangeSelector.
    * `ui/` — Card, Badge, Delta, StatCard, BarChart, Sparkline, TelemetryTrace, SpeedDeltaTrace,
      GearMismatchStrip, TrackMap, FilterSelect, Modal, SectionHeader, States, TrackPracticeChart.
    * Feature-specific — LapBrowser, LapPicker, SessionRow.
* **`screens/`** — Page-level components: OverviewScreen, SessionsScreen, SessionDetailScreen, LapsScreen,
  CompareScreen.
* **`providers/`** — DataModeProvider (mock/live toggle), TimeRangeProvider (global time range).
* **`hooks/`** — `useUrlState` (URL querystring read/write).
* **`lib/`** — Formatting utilities.
* **`config/`** — Navigation items.

### HATEOAS Integration

Unlike standard REST where URLs are hardcoded in the frontend, the React app follows `_links` provided in API
responses to navigate. `client.ts` exposes `fetchLink(ctx, links, rel)` for following arbitrary link relations.
If the backend changes its URL scheme, the frontend follows without code changes.

### Bootstrap: `GET /api/1`

The frontend boots from a single index resource at **`/api/1`** which returns an `IndexResource`
carrying two pieces of information that are deliberately kept separate:

- **`_links` — API capabilities.** Every rel is always present. Hooks follow these to fetch data,
  so a screen can consume data from another feature's API even when that feature's UI surface is
  hidden (e.g. Overview keeps showing recent sessions and per-track bests when the Sessions UI is
  disabled).
- **`enabledFeatures` — UI surfaces.** Lists the [Feature](../app/src/main/kotlin/com/github/prule/laptimeinsights/Feature.kt)
  ids whose nav items, routes, and cross-screen action buttons should render. Driven by
  `FEATURE_<NAME>` env vars layered on top of `Feature.defaultEnabled`.

This split fixes the failure mode where disabling, say, Sessions also broke Overview because
Overview's data fetches were gated on the same toggle. Capability and UI presence are now
independent.

Stable link relations: `self`, `overview`, `sessions`, `sessionOptions`, `sessionsAggregate`,
`laps`, `lapsAggregate`, `compare`, `live`.

### Sortable column discovery

Paged search responses (`GET /api/1/laps`, `GET /api/1/sessions`) carry a top-level
`sortable: string[]` field listing the field names accepted by the `sort` query parameter for that
collection. The UI uses this to enable/disable column headers and to build `sort=field:ASC|DESC`
without hard-coding the list.

The contract lives in the domain layer — `Lap.SORTABLE_FIELDS` /
`Session.SORTABLE_FIELDS` — and each persistence adapter (`LapEntity` / `SessionEntity`) validates
its Exposed column mapping covers exactly that set at class init. To add a new sortable column:
append the field name to the domain list, then add the matching `name to Column` entry in the
entity's `sortableFields` map.

Stable feature ids: `overview`, `sessions`, `laps`, `compare`, `live`.

**Frontend feature gating** is centralised in two places so adding a feature stays a one-line
change everywhere downstream:

- `frontend/src/config/features.tsx` — a `FEATURE_CONFIG` registry mapping each feature to its
  HATEOAS rel, sidebar nav config, and router routes.
- `frontend/src/providers/FeaturesProvider.tsx` — fetches `/api/1` via TanStack Query, exposes
  `useFeatures()` (returns `links` + `isEnabled`) and `useFeatureEnabled(feature)`. While the
  index is loading every feature is treated as on so the UI doesn't flicker.

Everything else reads from those two:

- `Sidebar` and `App.tsx` derive nav + routes from the registry filtered by `isEnabled` (UI).
- `api/queries.ts` is HATEOAS-first — every URL the hooks fetch comes from a link relation:
    - Listing hooks (`useSessions`, `useLaps`, `useSessionOptions`, `useTrackBestLap`,
      `useLapComparison`) follow the index `/api/1` `_links`.
    - Per-record hooks (`useSessionLaps(session)`, `useSessionBestLap(session)`,
      `useLapTelemetry(lap)`) follow `_links` on the parent resource. These rels are always
      present (capability), so the hook only short-circuits when the parent record hasn't loaded
      yet.
    - "By uid" entry points (`useSession(uid)`, `useLap(uid)`) compose `${indexLink}/{uid}`.
- Cross-screen action UI gates on the **UI feature** (`useFeatureEnabled`), not on link presence:
    - Per-record actions (clickable session column in a lap row, "go to session" buttons in
      tables) — clickable when the *target* UI feature is enabled.
    - Global actions ("View all" → `/sessions`, the Compare toolbar in `LapsScreen`,
      session-detail "Back to sessions") — same gate.

---

## 5. Data Flow: From Telemetry to Dashboard

### Live recording

1. **Ingestion:** `acc-client` emits `RealtimeCarUpdate` events at ~100 ms intervals per car.
2. **ClientInitializer:** `buildRealTimeCarUpdate()` maps the raw ACC message to a `RecordRealtimeCarUpdateCommand`.
3. **Use Case:** `RecordRealtimeCarUpdateService` persists the row to `REALTIME_CAR_UPDATE` via
   `CreateRealtimeCarUpdatePort`.
4. **Lap completion:** `buildLapCompleted()` receives a `LAPCOMPLETED` broadcast event, persists the lap via
   `CreateLapUseCase`, and emits a `LapCreated` domain event.
5. **WebSocket push:** `SessionEventController` forwards domain events to connected browser clients.
6. **Consumption:** The user navigates to Compare, picks two laps. The React app calls
   `GET /api/1/laps/compare?lap1Uid=…&lap2Uid=…`. The backend reads `REALTIME_CAR_UPDATE` rows for both lap UIDs and
   projects them into `TelemetrySample` objects (splinePosition, speedKph, gear, worldPosX, worldPosY).

### Mock mode

`DatabaseSeeder` generates synthetic `RealtimeCarUpdate` rows for each seeded lap. The frontend's `mock/data.ts`
mirrors the same algorithm so mock-mode charts look identical to live-mode charts.

---

## 6. Decisions & Non-Goals

### No PWA (Progressive Web App) — intentional

The frontend is **deliberately not** built as a PWA. This is a standing decision, not a gap;
compliance audits should treat the PWA criterion as **N/A** for this project. Rationale:

* **Self-hosted local app.** It runs locally against a self-hosted Ktor backend on the user's own
  LAN (see §1), not as a public HTTPS web app. The installable-web-app model PWAs exist for doesn't apply.
* **Requires a live local backend.** Every view depends on the local API and real-time ACC telemetry,
  so a service-worker offline cache adds no usable functionality.
* **Desktop-only.** It targets a desktop browser alongside the running sim; there is no mobile /
  add-to-home-screen use case.
* **Distributed as a release bundle.** Shipped as a packaged GitHub release; install and update are
  handled outside the browser, not via a web-app manifest.

## 7. Key Design Patterns

* **Screaming Architecture:** The package structure tells you what the app does (`com.laptime.sessions`)
  rather than how it works (`com.laptime.controllers`).
* **Ports & Adapters:** Every external dependency (DB, UDP client, WebSocket) is hidden behind an interface in
  `application.port`. Swapping the database or transport requires changing only the adapter, not the domain.
* **Custom Hooks:** Telemetry-sharing state (e.g., `hoveredPosition` on the Compare screen) is lifted to the nearest
  common ancestor and passed as props, following standard React patterns.
