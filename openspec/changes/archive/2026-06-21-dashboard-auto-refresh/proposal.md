## Why

The backend already broadcasts `LapCreated` / `Session*` domain events over the
`/api/1/events` WebSocket, but only `LiveScreen` consumes them. Every other screen
(Overview, Sessions, Laps, Session detail) reads through TanStack Query with no
WebSocket bridge and a 30 s `staleTime`, so a completed lap does not appear until the
user manually refreshes or navigates. The dashboard looks stale during an active
session — the exact moment the data matters most.

## What Changes

- Open the `/api/1/events` WebSocket **once** at the app level (shared connection,
  auto-reconnect) instead of only inside `LiveScreen`.
- On relevant events, invalidate the matching TanStack Query caches so any mounted
  screen refetches and re-renders automatically:
  - `LapCreated` → laps lists, lap aggregates, session-laps, session/track best-lap
  - `SessionCreated` / `SessionStarted` / `SessionUpdated` / `SessionEnded` →
    sessions list, session aggregates, the affected `session` record
- Refactor `LiveScreen` to consume the shared connection rather than opening its own
  second WebSocket (avoid two sockets per client).
- Gate the whole mechanism on LIVE data mode and the presence of the index `live` rel,
  so MOCK mode and feature-off backends are unaffected.

## Capabilities

### New Capabilities
- `dashboard-live-sync`: A shared app-level WebSocket subscription that translates
  backend domain events into TanStack Query cache invalidations, keeping every screen
  current without manual refresh.

### Modified Capabilities
<!-- None — no existing openspec/specs capability changes its requirements. -->

## Impact

- `frontend/src/main.tsx` — mount a new live-sync provider inside `QueryClientProvider`.
- New `frontend/src/providers/LiveEventsProvider.tsx` (or `hooks/`) — single WS + subscribe API.
- New cache-sync hook mapping event types → `queryClient.invalidateQueries` key prefixes.
- `frontend/src/screens/LiveScreen.tsx` — consume shared events instead of own socket.
- `frontend/src/api/queries.ts` — query keys are the invalidation contract (no behavior change, may export key prefixes).
- `docs/real-time-updates.md` — document the dashboard-wide sync.
- No backend changes; WebSocket contract is unchanged.
