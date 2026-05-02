# Real-time Updates

The Laptime Insights Server provides real-time updates for recorded sessions and laps using WebSockets and a Domain
Event system. This allows the web application to automatically update without manual refreshing.

## Architecture

The real-time system follows Clean Architecture principles:

1. **Domain Events**: Events like `SessionCreated`, `SessionStarted`, `SessionUpdated`, `SessionFinished`, and
   `LapCreated` are defined in the domain layer
   (`com.github.prule.laptimeinsights.application.domain.model.DomainEvent`).
2. **Event Port**: An `EventPort` interface in the application layer defines how events are emitted and observed.
3. **In-Memory Event Adapter**: An implementation of `EventPort` using Kotlin's `SharedFlow` to broadcast events across
   the application.
4. **Service Integration**: Domain services (`CreateSessionService`, `StartSessionService`, `UpdateSessionService`,
   `FinishSessionService`, `CreateLapService`) emit events to the `EventPort` after successfully persisting data to
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
| `SessionFinished`  | `SessionFinished` | `SessionResource`  |
| `LapCreated`       | `LapCreated`      | `LapResource`      |

`SessionCreated` is emitted immediately after persistence, when `startedAt` is still `null`.
`SessionStarted` follows once telemetry confirms the session has begun and `startedAt` has been
populated. Clients should treat the latter as the signal that the session is "live", not the
former.

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

### `SessionCreated` / `SessionStarted` / `SessionUpdated` / `SessionFinished`

```json
{
  "type": "SessionCreated",
  "data": {
    "uid": "...",
    "startedAt": "2026-04-12T09:55:00Z",
    "endedAt": null,
    "simulator": "ACC",
    "track": "Monza",
    "car": "Ferrari 488 GT3 Evo",
    "sessionType": "Race",
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
        case 'SessionFinished':
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
