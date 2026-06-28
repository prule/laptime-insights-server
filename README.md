# laptimeinsights.com - An Assetto Corsa Competizione dashboard

https://laptimeinsights.com

This repository is the server component for the LapTimeInsights dashboard. It is a work in progress, currently under
development.

> **Deployment model:** LapTimeInsights is **self-hosted**. The user **downloads it and runs it locally on their own
> network — on the same network as their ACC server**. It is not a cloud/SaaS product, and telemetry never leaves the
> user's network. The dashboard is then opened in a browser from that PC or any device on the same network.

The server uses [acc-client](https://github.com/prule/acc-client) to listen to telemetry from Assetto Corsa
Competizione (ACC) and records information in a database. The server implements a REST API so the frontend can render
data and provide insights to the user.

The dashboard intends to:

- Show amount of effort put it
    - Record sessions to display how often the user is racing.
    - Record laps to display how many laps the user has completed and distance covered
- Display lap information and let the user compare their lap with other drivers
    - Compare gear changes and speed at certain parts of the track

And much more hopefully!

> Mostly this is an exercise in practicing software development, practicing clean architecture, implementing libraries
> and distributing applications.

## Documentation

- [User Guide](./docs/user-guide.md) - How to use the dashboard (Overview, Sessions, Laps, Compare).
- [Frontend Technical](./docs/frontend-technical.md) - Frontend architecture, screens, providers, and dev workflows.
- [Clean Architecture](./docs/clean-architecture.md) - Backend project structure and conventions.
- [Real-time Updates](./docs/real-time-updates.md) - WebSocket event stream protocol.

### Frontend

A React + Vite + TypeScript dashboard lives in `frontend/`. See
[frontend/README.md](./frontend/README.md) for full documentation.

**Toolchain:** Node 24 (`.node-version` pins `24.17.0`) and pnpm. Both JS workspaces
(`frontend/` and `landing/`) declare these in `engines` and run a `preinstall`
guard, so `pnpm install` fails fast on an older Node. Run `fnm use` first.

Quick start:

```bash
cd frontend
fnm use      # selects Node 24 from .node-version
pnpm install
pnpm dev   # http://localhost:5173 — defaults to mock data
```

The sidebar toggle switches between **mock** mode (in-memory data mirroring
the backend seeder) and **live** mode (real HTTP calls, proxied to the Ktor
backend on port 8000 by Vite). Mock data shape matches the backend exactly,
including HATEOAS `_links`, so screens behave identically across modes.

### Landing page

A standalone marketing site for laptimeinsights.com lives in `landing/` — plain
HTML + Tailwind, deployed to Cloudflare Pages, fully isolated from `app/` and
`frontend/`. See [landing/README.md](./landing/README.md).

```bash
cd landing
pnpm install
pnpm build   # outputs static site to landing/dist
```

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JDBC_URL` | yes | JDBC connection string for the H2 database (e.g. `jdbc:h2:./data/laptime;DB_CLOSE_DELAY=-1`). |
| `DB_SEED` | no | Set to `true`, `1`, or `yes` to populate the database with sample sessions and laps on startup. The seeder is idempotent — it skips when data already exists. Intended for local development and demos only. |
| `FEATURE_OVERVIEW` | no | Feature override. Truthy (`true`/`1`/`yes`) forces on, falsy (`false`/`0`/`no`) forces off, unset uses `Feature.OVERVIEW.defaultEnabled` from `Feature.kt`. Controls the Overview link in `GET /api/1`. |
| `FEATURE_SESSIONS` | no | Feature override. Same semantics as `FEATURE_OVERVIEW`. Controls the `sessions` / `sessionOptions` / `sessionsAggregate` links. |
| `FEATURE_LAPS` | no | Feature override. Same semantics. Controls the `laps` / `lapsAggregate` links. |
| `FEATURE_COMPARE` | no | Feature override. Same semantics. Controls the `compare` link. |
| `FEATURE_LIVE` | no | Feature override. Same semantics. Controls the `live` WebSocket link. |

These are **backend** (runtime) variables. The frontend has separate **build-time**
`VITE_FEEDBACK_*` variables that wire the in-dashboard feedback form to a Google Form; because
Vite inlines them at build time, they must be set on whichever build you distribute. See
[frontend/README.md → Feedback form](./frontend/README.md#feedback-form).

### REST API

The REST API is self-documenting via Ktor's OpenAPI plugin. When the server is running locally:

- **OpenAPI document** — `http://localhost:<port>/openapi`
- **Swagger UI** — `http://localhost:<port>/swaggerUI`

Operation-level documentation (summary, description, query parameters, responses) is attached to
each route in its inbound adapter (e.g. `SearchSessionController`) using the experimental
`describe { }` DSL from `io.ktor.server.routing.openapi`. Whenever a route is added or its
parameters change, update the matching `describe { }` block so the OpenAPI spec stays in sync with
the code.

The real-time WebSocket endpoint at `ws://<host>:<port>/api/1/events` is **not** part of the
OpenAPI document (OpenAPI 3.x has no first-class WebSocket support). Its protocol is documented in
[Real-time Updates](./docs/real-time-updates.md) and in the KDoc on `SessionEventController`.
