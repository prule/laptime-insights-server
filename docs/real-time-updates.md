# Real-time Updates

The Laptime Insights Server provides real-time updates for recorded sessions and laps using WebSockets and a Domain
Event system. This allows the web application to automatically update without manual refreshing.

## Architecture

The real-time system follows Clean Architecture principles:

1. **Domain Events**: Events like `SessionCreated`, `SessionStarted`, `SessionUpdated`, and
   `LapCreated` are defined in the domain layer
   (`com.github.prule.laptimeinsights.application.domain.model.DomainEvent`).
2. **Event Port**: An `EventPort` interface in the application layer defines how events are emitted and observed.
3. **In-Memory Event Adapter**: An implementation of `EventPort` using Kotlin's `SharedFlow` to broadcast events across
   the application.
4. **Service Integration**: Domain services (`CreateSessionService`, `StartSessionService`, `UpdateSessionService`,
   `CreateLapService`) emit events to the `EventPort` after successfully persisting data to
   the database.
5. **WebSocket Controller**: The `SessionEventController` in the web adapter layer exposes a WebSocket endpoint that
   collects events from the `EventPort`, wraps them in a typed `WebSocketMessage` envelope, and pushes them to
   connected clients.

## Connecting to the Event Stream

Clients can subscribe to all events by connecting to the following WebSocket endpoint:

**Endpoint:** `ws://[hostname]:[port]/api/1/events`

> **Note:** WebSocket endpoints are not described in the OpenAPI document at `/openapi` or in
> Swagger UI at `/swaggerUI` — OpenAPI 3.x has no first-class WebSocket support. The KDoc on
> `SessionEventController` and this page are the canonical contract.

### Forwarded events

The following domain events are forwarded to WebSocket clients:

| Domain event       | Envelope `type`   | `data` payload     |
| ------------------ | ----------------- | ------------------ |
| `SessionCreated`   | `SessionCreated`  | `SessionResource`  |
| `SessionStarted`   | `SessionStarted`  | `SessionResource`  |
| `SessionUpdated`   | `SessionUpdated`  | `SessionResource`  |
| `LapCreated`       | `LapCreated`      | `LapResource`      |

`SessionCreated` is emitted immediately after persistence, when `startedAt` is still `null`.
`SessionStarted` follows once telemetry confirms the session has begun and `startedAt` has been
populated. Clients should treat the latter as the signal that the session is "live", not the
former.

Sessions have no explicit "finished" event — activity is reflected in the `drivingTimeMs`
aggregate on the session, which grows as each player lap is recorded and is delivered with
every `SessionUpdated` (and via REST). Clients that previously waited for `SessionFinished`
should instead observe that no further `LapCreated` / `PlayerCarUpdated` events arrive for
the active session.

To broadcast a new domain event, add a subclass to `WebSocketMessage` (in
`adapter/in/web/session/`) and a matching branch to the `when` in `SessionEventController`.

## Frame format

Every frame is a single JSON object using a `type` / `data` envelope:

```json
{
  "type": "<event name>",
  "data": { ...resource fields... }
}
```

The `type` field is the stable wire identifier — clients dispatch on it. The `data` field
contains the corresponding REST resource verbatim, including its `_links` HATEOAS field.

### `SessionCreated` / `SessionStarted` / `SessionUpdated`

```json
{
  "type": "SessionCreated",
  "data": {
    "uid": "...",
    "startedAt": "2026-04-12T09:55:00Z",
    "simulator": "ACC",
    "track": "Monza",
    "car": "Ferrari 488 GT3 Evo",
    "sessionType": "Race",
    "playerCarId": 13,
    "drivingTimeMs": 0,
    "_links": {
      "self": "/api/1/sessions/..."
    }
  }
}
```

### `LapCreated`

```json
{
  "type": "LapCreated",
  "data": {
    "uid": "...",
    "sessionUid": "...",
    "recordedAt": "2026-04-12T10:00:00Z",
    "lapTime": 124500,
    "lapNumber": 5,
    "valid": true,
    "personalBest": false,
    "_links": {
      "self": "/api/1/laps/..."
    }
  }
}
```

### Example Implementation (JavaScript)

```javascript
const socket = new WebSocket('ws://localhost:8080/api/1/events');

socket.onopen = () => {
    console.log('Connected to real-time event stream');
};

socket.onmessage = (event) => {
    const message = JSON.parse(event.data);

    switch (message.type) {
        case 'SessionCreated':
        case 'SessionStarted':
        case 'SessionUpdated':
            handleSessionEvent(message.type, message.data);
            break;
        case 'LapCreated':
            handleLapEvent(message.data);
            break;
        default:
            console.warn('Unknown event type:', message.type);
    }
};

function handleSessionEvent(type, session) {
    console.log(type, ':', session.simulator, 'at', session.track);
    // Update your UI components
}

function handleLapEvent(lap) {
    console.log('New Lap:', lap.lapNumber, 'Time:', lap.lapTime);
    // Update your UI components
}

socket.onerror = (error) => {
    console.error('WebSocket Error:', error);
};

socket.onclose = () => {
    console.log('Disconnected from event stream');
};
```

## Mid-session catch-up

Connecting clients receive only events that fire *after* the WebSocket opens.
A page loaded mid-session therefore has no record of laps recorded earlier.

The frontend `LiveScreen` resolves this with two REST fetches alongside the WS:

1. **On mount** — fetch the most recent session via
   `GET /api/1/sessions?sort=startedAt:DESC&size=1`, seed `session` state, and fetch its laps via
   `GET /api/1/laps?sessionUid={uid}&carId={playerCarId}&sort=lapNumber:DESC`. (There is no
   server-side "finished" flag — the most recent session is treated as the live one.)
2. **On every `LapCreated`** — prepend the new lap optimistically, then
   re-fetch the same lap list to overwrite local state with the authoritative
   server view (handles dedup and any laps the client missed).

A `PlayerCarUpdated` event for an unknown session also triggers a one-off
`GET /api/1/sessions/{uid}` + lap fetch so a page that connects after a fresh
session has started still picks up its details.
