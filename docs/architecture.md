This architecture document outlines the technical blueprint for the **LapTimeInsights** ecosystem. It adheres to **Clean
Architecture** principles to ensure the business logic (telemetry analysis) remains decoupled from the infrastructure (
Ktor, Exposed, React).

---

## 1. System Overview

LapTimeInsights consists of a **Kotlin/Ktor** server acting as a local hub and a **React/TypeScript** dashboard.

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
carrying only `_links`. Each link relation corresponds to a high-level [Feature](../app/src/main/kotlin/com/github/prule/laptimeinsights/Feature.kt)
(`overview`, `sessions`, `laps`, `compare`, `live`); a feature toggled off via its `FEATURE_<NAME>`
environment variable has its link omitted, signalling the UI to hide the matching nav item. All
features default to enabled — set e.g. `FEATURE_LIVE=false` to disable.

Stable link relations: `self`, `overview`, `sessions`, `sessionOptions`, `laps`, `compare`,
`live`.

**Frontend feature gating** is centralised in two places so adding a feature stays a one-line
change everywhere downstream:

- `frontend/src/config/features.tsx` — a `FEATURE_CONFIG` registry mapping each feature to its
  HATEOAS rel, sidebar nav config, and router routes.
- `frontend/src/providers/FeaturesProvider.tsx` — fetches `/api/1` via TanStack Query, exposes
  `useFeatures()` / `useFeatureEnabled(feature)`. While the index is loading every feature is
  treated as on so the UI doesn't flicker; the response then prunes anything the backend hasn't
  advertised.

Everything else reads from those two:

- `Sidebar` and `App.tsx` derive nav + routes from the registry filtered by `isEnabled`.
- `api/queries.ts` is HATEOAS-first — every URL the hooks fetch comes from a link relation:
    - Listing hooks (`useSessions`, `useLaps`, `useSessionOptions`, `useTrackBestLap`,
      `useLapComparison`) follow the index `/api/1` `_links`.
    - Per-record hooks (`useSessionLaps(session)`, `useSessionBestLap(session)`,
      `useLapTelemetry(lap)`) follow `_links` on the parent resource — so when the backend
      omits a cross-feature rel because that feature is off, the hook short-circuits to
      `enabled: false` automatically.
    - "By uid" entry points (`useSession(uid)`, `useLap(uid)`) compose `${indexLink}/{uid}`.
- Cross-screen action UI:
    - Per-record actions (clickable session column in a lap row, "go to session" buttons in
      tables) gate on `record._links[rel]` presence — backend has already decided whether the
      target feature is reachable for that record.
    - Global actions ("View all" → `/sessions`, the Compare toolbar in `LapsScreen`,
      session-detail "Back to sessions") gate on `useFeatureEnabled(feature)` since they
      aren't tied to one record.

Backend-side: `Application.setEnabledFeatures(...)` (called once in `module(...)`) is read by
`SessionLinkFactory`, `LapLinkFactory`, and `SessionOptionsLinkFactory` to decide which
cross-feature rels to emit. The result is that *link presence is the gate* — the frontend never
needs to consult a separate env-var mirror or feature flag map.

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

## 6. Key Design Patterns

* **Screaming Architecture:** The package structure tells you what the app does (`com.laptime.sessions`)
  rather than how it works (`com.laptime.controllers`).
* **Ports & Adapters:** Every external dependency (DB, UDP client, WebSocket) is hidden behind an interface in
  `application.port`. Swapping the database or transport requires changing only the adapter, not the domain.
* **Custom Hooks:** Telemetry-sharing state (e.g., `hoveredPosition` on the Compare screen) is lifted to the nearest
  common ancestor and passed as props, following standard React patterns.
