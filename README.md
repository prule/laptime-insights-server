# laptimeinsights.com - An Assetto Corsa Competizione dashboard

https://laptimeinsights.com

This repository is the server component for the LapTimeInsights dashboard. It is a work in progress, currently under
development. This software would be installed locally on the users network to communicate with the ACC server.

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

- [Clean Architecture](./docs/clean-architecture.md) - Details on the project structure and conventions.
- [Real-time Updates](./docs/real-time-updates.md) - How real-time events and WebSockets are implemented.

### Frontend

A React + Vite + TypeScript dashboard lives in `frontend/`. See
[frontend/README.md](./frontend/README.md) for full documentation. Quick start:

```bash
cd frontend
npm install
npm run dev   # http://localhost:5173 — defaults to mock data
```

The sidebar toggle switches between **mock** mode (in-memory data mirroring
the backend seeder) and **live** mode (real HTTP calls, proxied to the Ktor
backend on port 8000 by Vite). Mock data shape matches the backend exactly,
including HATEOAS `_links`, so screens behave identically across modes.

### Environment variables

| Variable | Required | Description |
|----------|----------|-------------|
| `JDBC_URL` | yes | JDBC connection string for the H2 database (e.g. `jdbc:h2:./data/laptime;DB_CLOSE_DELAY=-1`). |
| `DB_SEED` | no | Set to `true`, `1`, or `yes` to populate the database with sample sessions and laps on startup. The seeder is idempotent — it skips when data already exists. Intended for local development and demos only. |

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
