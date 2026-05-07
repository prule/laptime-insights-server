# Specification: Live Page

## Goal

Give the driver a real-time, glanceable view of the in-progress ACC session: the
current car telemetry, the lap times completed so far, and the shape of the
track being driven. Intended to be left up on a second monitor while driving.

## Functional Requirements

- **Live mode gating** — the page is only meaningful when the frontend is in
  `live` data mode (see `DataModeProvider`). In `mock` mode, render a placeholder
  prompting the user to switch modes via the sidebar toggle.
- **Header** — show track name and session type (e.g. `Snetterton · Practice`)
  for the active session, plus a connection-status badge.
- **HUD** (gear, speed, position) — large, monospaced cards updated at the rate
  of the inbound `PlayerCarUpdated` stream.
- **Lap-time row** (current, best, delta) — same telemetry source. Sentinel
  values render as `—`.
- **Track-map** — incrementally trace the player car's path through the lap
  using ACC world-space coordinates and overlay a moving dot at the current
  position.
- **Completed laps table** — the list of laps recorded so far for the player
  car in the active session, newest first, showing lap number, lap time, status
  (PB / INVALID / —), and a PB tick column. Server is the source of truth for
  `valid` and `personalBest`; the client renders verbatim.
- **Mid-session catch-up** — a page loaded after the session has already started
  populates immediately rather than staying empty until the next event arrives.
- **Resilient connection** — transparently reconnect on WebSocket drop; show
  the connection state in the header.

## Data Sources

- **WebSocket**: `ws://<host>:<port>/api/1/events` — see
  [Real-time Updates](../real-time-updates.md). Frames consumed:
  `ServerStarted`, `SessionCreated`, `SessionStarted`, `SessionUpdated`,
  `LapCreated`, `PlayerCarUpdated`. Sessions have no explicit "finished" frame —
  activity is implied by the absence of further laps / telemetry, and the
  cumulative `drivingTimeMs` aggregate carried on the session.
- **REST** (used to seed state when the WS connects mid-session):
  - `GET /api/1/sessions?sort=startedAt:DESC&size=1` — most recent session.
  - `GET /api/1/sessions/{uid}` — single-session details.
  - `GET /api/1/laps?sessionUid={uid}&carId={playerCarId}&sort=lapNumber:DESC&size=200`
    — full lap list for the player car in a session.

## Connection Lifecycle

| Status         | Trigger                                                         | UI |
|----------------|------------------------------------------------------------------|----|
| `connecting`   | Initial mount, or after a closed socket pending reconnect        | Pulsing yellow dot, label `CONNECTING` |
| `connected`    | `WebSocket.onopen`                                               | Glowing green dot, label `CONNECTED` |
| `disconnected` | `WebSocket.onclose` (without explicit unmount)                   | Muted dot, label `DISCONNECTED` |
| `error`        | `WebSocket.onerror`                                              | Red dot, label `ERROR` |

After a `disconnected` close that the client did not initiate, the page
automatically schedules a reconnect after **3 seconds**. Unmounting the screen
sets a `closed` flag so the close handler does not retry.

## State Lifecycle (per WebSocket frame)

| Frame              | Action |
|--------------------|--------|
| `ServerStarted`    | Clear all in-memory state (session, telemetry, laps, track points, refs). The server is fresh; anything we held was stale. |
| `SessionCreated`, `SessionStarted`, `SessionUpdated` | Replace `session`. Cache `playerCarId` in a ref. If `session.uid` changed, reset laps, track points, and telemetry — a new session has begun. |
| `LapCreated`       | Optimistically prepend the lap to local state (deduped by uid), then re-fetch the full server-side lap list for `(sessionUid, playerCarId)` and replace local state with it. |
| `PlayerCarUpdated` | Replace `telemetry`. If the message is for a session we have not seen before (no prior lifecycle frame), fetch the session via REST and seed the lap list at the same time. Append the world-space coordinate to the track-points buffer; sync to React state every 5 samples; cap the buffer at 2 000 points. |

## Mid-Session Catch-Up

The WebSocket only delivers events that fire after it opens, so a page loaded
mid-session has no record of laps recorded earlier. Three independent fetches
fill the gap:

1. **On mount** — `GET /api/1/sessions?sort=startedAt:DESC&size=1`, seed
   `session` state, and fetch its lap list. (Sessions no longer carry a
   "finished" flag — the most recent one is treated as the live session; if
   nothing is currently being driven the page just sits idle waiting for the
   next telemetry frame.)
2. **On every `LapCreated`** — re-fetch the full lap list to overwrite local
   state with the authoritative server view (handles dedup, demoted PBs after
   a faster lap is recorded, and any laps the client missed).
3. **On a `PlayerCarUpdated` for an unknown session** — fetch the session via
   `GET /api/1/sessions/{uid}` and seed its lap list.

Failed fetches are silent — the page still surfaces whatever WS-driven state
arrives subsequently.

## Player-Car Filtering

`session.playerCarId` is the ACC car index of the focused (player) car. The
laps table only renders laps whose `(sessionUid, carId)` match the active
session and that index. While `playerCarId` is null (e.g. before the first
`EntryListCar` arrives), the filter falls back to the full lap list for the
session.

## HUD Behaviour

- **Gear** — `0` renders as `R`, `1` as `N`, `>=2` as `gear - 1`. The card
  switches to red accent styling when the current lap is flagged invalid.
- **Speed** — `kmh` integer rendered in cyan.
- **Position** — `racePosition` rendered as `P{n}` in red accent.
- **Current lap** — formatted lap time; renders `0:00.000` when 0 or negative.
  Red accent if the current lap is invalid.
- **Best lap** — formatted lap time; renders `—` when the telemetry sentinel
  (`Long.MAX_VALUE`, treated as `>= Number.MAX_SAFE_INTEGER` on the client) is
  in effect.
- **Delta** — `±0.000` for zero, `+x.xxx` red for positive, `-x.xxx` green for
  negative. Renders `—` while the best-lap sentinel is set.

## Track Map

- **Source data** — every `PlayerCarUpdated` contributes a `(worldPosX,
  worldPosY)` sample. A ring buffer (capped at 2 000 points) keeps memory
  bounded.
- **Outline** — the polyline through all accumulated points. Bounds are
  computed once per recompute and cached so the outline does not jitter as
  the dot moves; the moving dot is positioned within the cached bounds.
- **Reflow cadence** — the React state copy is only updated every 5 samples to
  avoid forcing an outline recompute on every frame. The current-position dot
  recalculates cheaply on every frame.
- **Empty state** — fewer than 5 points renders the placeholder `Accumulating
  track data…`. Fewer than 2 points yields no outline.
- **Reset** — the buffer is cleared on `ServerStarted` and on a
  `SessionCreated/Started/Updated` whose `session.uid` differs from the
  previous one.

## Completed Laps Table

- Sorted **newest first** (the server returns `lapNumber:DESC`; the optimistic
  prepend on `LapCreated` matches that order).
- Filtered to the active session's `playerCarId` when known.
- Columns: `Lap` (`#{lapNumber}`), `Lap Time` (`mm:ss.mmm`), `Status`
  (`PB` / `INVALID` / `—`), `PB` (✓ when `personalBest`).
- Lap-time colour: green if PB, default text colour if valid non-PB, dim if
  invalid.
- Empty state: `No laps completed yet`.
- The client treats `valid` and `personalBest` as authoritative server fields
  and never derives them locally. PB derivation lives in
  `CreateLapService.kt`.

## Ambient Caveat

ACC's broadcast protocol does not include throttle or brake inputs, so the HUD
is limited to gear, speed, position, and lap times — no pedal traces.

## Data Mode Gate

When `mode === "mock"`, the page short-circuits to a placeholder card pointing
at the sidebar toggle. The mock backend does not emit WebSocket frames; the
Live page is meaningful only against a real backend.

## Implementation

- Single screen file: `frontend/src/screens/LiveScreen.tsx`.
- Telemetry envelope and `PlayerCarUpdateData` shape mirror
  `app/.../adapter/in/web/session/WebSocketMessage.kt`.
- Lifecycle `useLiveEvents(apiUrl)` hook owns connection state, refs (last
  session uid, player car id, raw track-points buffer), and dispatches the
  REST seeding fetches.
- The page itself is presentational: header + connection badge, optional HUD
  block (only when telemetry is present), optional track map, and the laps
  table.
- Sub-components: `ConnectionBadge`, `GearDisplay`, `SpeedDisplay`,
  `PositionDisplay`, `LiveTrackMap`.

## Out of Scope (current implementation)

- No throttle / brake traces — the upstream ACC protocol does not expose them.
- No multi-car HUD — the HUD always reflects the focused (player) car.
- No driver-name or team-name display.
- No replay controls — the page is strictly live.
- No persistence of HUD state across reloads beyond what the server already
  knows; the page reconstitutes itself from REST + WS on every mount.
