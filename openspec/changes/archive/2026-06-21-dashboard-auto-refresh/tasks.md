## 1. Shared live-event connection

- [x] 1.1 Add `LiveEventsProvider` in `frontend/src/providers/` that opens one WebSocket to the index `live` rel, only when `mode === "live"` and `links.live` is present.
- [x] 1.2 Implement connect + 3 s auto-reconnect (lift the logic from `LiveScreen.useLiveEvents`); expose connection status and a `subscribe(listener) => unsubscribe` API via context.
- [x] 1.3 Be inert in MOCK mode / when `live` rel absent (no socket, no listeners).
- [x] 1.4 Mount the provider in `frontend/src/main.tsx` inside `QueryClientProvider` (above `App`).

## 2. Event → cache invalidation

- [x] 2.1 Add a `useLiveCacheSync` hook that subscribes to the provider and maps event types to `queryClient.invalidateQueries` calls by key prefix.
- [x] 2.2 `LapCreated` → invalidate `laps`, `laps-aggregate`, `session-laps`, `session-best-lap`, `track-best-lap`.
- [x] 2.3 `SessionCreated|SessionStarted|SessionUpdated|SessionEnded` → invalidate `sessions`, `sessions-aggregate`, and `session`.
- [x] 2.4 Do NOT invalidate on `PlayerCarUpdated` (high-frequency telemetry, LiveScreen-local only).
- [x] 2.5 Mount `useLiveCacheSync` from the provider so it runs app-wide.

## 3. Refactor LiveScreen onto the shared connection

- [x] 3.1 Change `useLiveEvents` to consume the shared `subscribe` instead of `new WebSocket(...)`.
- [x] 3.2 Verify telemetry, optimistic lap prepend, REST catch-up, and track-map behavior are unchanged; confirm only one socket is open.

## 4. Tests

- [x] 4.1 Test that a `LapCreated` event invalidates the lap-derived query prefixes (mock `queryClient.invalidateQueries`).
- [x] 4.2 Test that session lifecycle events invalidate the session-derived prefixes.
- [x] 4.3 Test the provider opens no socket in MOCK mode / when `live` rel is absent.
- [x] 4.4 `pnpm typecheck`, `pnpm test`, `pnpm lint`, `pnpm build` pass.

## 5. Docs

- [x] 5.1 Update `docs/real-time-updates.md` to document the app-wide cache-sync layer (not just LiveScreen).
- [x] 5.2 Log any cleanup found during refactor in `docs/technical-debt.md`; mark resolved items DONE.
