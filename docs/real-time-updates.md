# Real-time Updates

The Laptime Insights Server provides real-time updates for recorded sessions and laps using WebSockets and a Domain
Event system. This allows the web application to automatically update without manual refreshing.

## Architecture

The real-time system follows Clean Architecture principles:

1. **Domain Events**: Events like `SessionCreated` and `LapCreated` are defined in the domain layer (
   `com.github.prule.laptimeinsights.application.domain.model.DomainEvent`).
2. **Event Port**: An `EventPort` interface in the application layer defines how events are emitted and observed.
3. **In-Memory Event Adapter**: An implementation of `EventPort` using Kotlin's `SharedFlow` to broadcast events across
   the application.
4. **Service Integration**: Domain services (like `CreateSessionService` and `CreateLapService`) emit events to the
   `EventPort` after successfully persisting data to the database.
5. **WebSocket Controller**: The `SessionEventController` in the web adapter layer exposes a WebSocket endpoint that
   collects events from the `EventPort` and pushes them to connected clients.

## Connecting to the Event Stream

Clients can subscribe to all events by connecting to the following WebSocket endpoint:

**Endpoint:** `ws://[hostname]:[port]/api/1/events`

### Example Implementation (JavaScript)

```javascript
const socket = new WebSocket('ws://localhost:8080/api/1/events');

socket.onopen = () => {
    console.log('Connected to real-time event stream');
};

socket.onmessage = (event) => {
    const data = JSON.parse(event.data);

    // Check the type of resource received
    if (data.lapTime !== undefined) {
        handleNewLap(data);
    } else if (data.simulator !== undefined) {
        handleNewSession(data);
    }
};

function handleNewLap(lap) {
    console.log('New Lap:', lap.lapNumber, 'Time:', lap.lapTime);
    // Update your UI components
}

function handleNewSession(session) {
    console.log('New Session Started:', session.simulator, 'at', session.track);
    // Update your UI components
}

socket.onerror = (error) => {
    console.error('WebSocket Error:', error);
};

socket.onclose = () => {
    console.log('Disconnected from event stream');
};
```

## Event Data Structure

The data sent over the WebSocket matches the standard REST API resources (`SessionResource` and `LapResource`).

### Lap Resource Example

```json
{
  "uid": "...",
  "sessionUid": "...",
  "recordedAt": "2023-10-27T10:00:00Z",
  "lapTime": 124500,
  "lapNumber": 5,
  "valid": true,
  "personalBest": false,
  "_links": {
    "self": "/api/1/laps/..."
  }
}
```

### Session Resource Example

```json
{
  "uid": "...",
  "startedAt": "2023-10-27T09:55:00Z",
  "simulator": "ACC",
  "track": "Monza",
  "car": "Ferrari 488 GT3 Evo",
  "_links": {
    "self": "/api/1/sessions/..."
  }
}
```
