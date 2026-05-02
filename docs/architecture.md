This architecture document outlines the technical blueprint for the **LapTimeInsights** ecosystem. It adheres to **Clean
Architecture** principles to ensure the business logic (telemetry analysis) remains decoupled from the infrastructure (
Ktor, Exposed, Vue).

---

## 1. System Overview

LapTimeInsights consists of a **Kotlin/Ktor** server acting as a local hub and a **Vue/TypeScript** dashboard.

* **Backend:** Handles telemetry ingestion via `acc-client`, persists data using **Exposed**, and serves a *
  *REST/HATEOAS** API.
* **Frontend:** A reactive dashboard built with **Vue 3** (Composition API) and **Pinia** for state management,
  consuming the HATEOAS API to drive navigation.

---

## 2. Backend Architecture: Clean & Hexagonal

The server is split into four distinct layers. Dependencies point **inwards** only.

### I. Domain Layer (The Core)

* **Entities:** Pure Kotlin data classes (e.g., `Lap`, `Session`, `TelemetryPoint`).
* **Repository Interfaces:** Boundary definitions (e.g., `LapRepository`, `SessionRepository`).
* **Business Logic:** Logic that doesn't belong in a use case, such as calculating "Effort Score" or "Grip Multipliers."

### II. Use Case Layer (Application)

* **Interactors:** Orchestrate the flow of data (e.g., `RecordLapUseCase`, `CompareLapsUseCase`).
* **Ports:** Input ports (Use Case interfaces) and Output ports (Repository interfaces).

### III. Interface Adapters (The Glue)

* **Controllers/Routes:** Ktor routing definitions.
* **HATEOAS Presenters:** Converts Domain Entities into **HAL (Hypertext Application Language)** JSON.
    * *Example:* A Lap resource includes a `_links` object with a URI to its `telemetry` or its parent `session`.
* **Repositories Implementation:** **Exposed** DAO/DSL logic that interacts with the database.

### IV. Infrastructure Layer

* **Ktor Server:** Configuration, Plugins (Authentication, ContentNegotiation, CORS).
* **Telemetry Client:** Integration with `acc-client` to bridge external UDP/Shared Memory data into the app.

---

## 3. Backend Technology Stack

| Component                | Technology                                       |
|:-------------------------|:-------------------------------------------------|
| **Language**             | Kotlin 2.x                                       |
| **Web Framework**        | Ktor (Server-side)                               |
| **ORM / Database**       | JetBrains Exposed / SQLite (Local) or PostgreSQL |
| **API Style**            | REST with HATEOAS (HAL+JSON)                     |
| **Dependency Injection** | Koin (Lightweight and Kotlin-native)             |

---

## 4. Frontend Architecture: Domain-Driven Vue

The frontend mirrors the backend's modularity by grouping files by **Domain** rather than technical type.

### Directory Structure (`src/`)

* **`modules/`**: Feature-based folders (e.g., `modules/sessions/`, `modules/analysis/`).
    * `components/`: Vue components specific to this feature.
    * `store/`: Pinia store for this feature's state.
    * `services/`: Axios/Fetch wrappers.
* **`core/`**: Shared types, HATEOAS link parsers, and global composables.
* **`views/`**: Page-level components that assemble modules.

### HATEOAS Integration

Unlike standard REST where URLs are hardcoded in the frontend, the Vue app will:

1. Fetch the **Entry Point** (e.g., `/api/v1/`).
2. Follow links provided in the `_links` property to navigate (e.g., `response._links.latest_session.href`).
3. This makes the frontend resilient to backend URI changes.

---

## 5. Data Flow: From Telemetry to Dashboard

1. **Ingestion:** `acc-client` emits a "Lap Completed" event.
2. **Use Case:** `RecordLapUseCase` is triggered.
3. **Persistence:** The Repository saves the lap and telemetry to the DB via **Exposed**.
4. **Notification:** (Optional) A WebSocket push alerts the **Vue** dashboard of the new lap.
5. **Consumption:** The user clicks the lap in the dashboard. The Vue app follows the `_links.telemetry` URI to fetch
   the data for the comparison chart.

---

## 6. Key Design Patterns

* **Screaming Architecture:** The package structure should tell you exactly what the app does (`com.laptime.sessions`)
  rather than how it works (`com.laptime.controllers`).
* **Result Pattern:** Use a `Result<T>` or `Either<L, R>` type for Use Case returns to handle errors (e.g., "Lap Not
  Found") without throwing exceptions across layers.
* **Composition API & Composables:** Encapsulate telemetry-sharing logic in `useTelemetry()` or `useSessionList()` hooks
  in Vue.
