## Context

The backend emits domain events (`LapCreated`, `SessionCreated/Started/Updated/Ended`,
`PlayerCarUpdated`) over the `/api/1/events` WebSocket (`docs/real-time-updates.md`).
Only `LiveScreen` opens that socket today; it manages its own `useState`-based
session/lap/telemetry model. Every other screen reads through TanStack Query hooks in
`frontend/src/api/queries.ts`, with `staleTime: 30_000` and `refetchOnWindowFocus:
false` (`main.tsx`). There is no bridge from WS events to the query cache, so a new lap
is invisible to Overview/Sessions/Laps/SessionDetail until manual refresh.

Query keys are already structured by prefix: `["laps", ...]`, `["sessions", ...]`,
`["session-laps", ...]`, `["laps-aggregate", ...]`, `["sessions-aggregate", ...]`,
`["session", ...]`, `["session-best-lap", ...]`, `["track-best-lap", ...]`. Each key also
carries `ctx.mode` and `ctx.apiBase`. This prefix structure is the invalidation surface.

## Goals / Non-Goals

**Goals:**
- One WebSocket per client; every screen stays current automatically in LIVE mode.
- Event → cache invalidation mapping driven by existing query-key prefixes.
- No backend changes; reuse the existing WS contract.
- MOCK mode and feature-off backends behave exactly as today.

**Non-Goals:**
- Optimistic cache patching of individual records (invalidate + refetch is sufficient and
  simpler; `LiveScreen` keeps its own optimistic prepend for the live table).
- Changing `staleTime` defaults or polling fallback.
- Backend event additions or a catch-up/replay protocol.

## Decisions

**1. Shared connection: app-level `LiveEventsProvider` over per-screen sockets.**
Mount a provider inside `QueryClientProvider` in `main.tsx`. It opens the single WS
(reusing `LiveScreen`'s connect/reconnect logic — derive ws URL from index `live` rel,
3 s reconnect) and exposes a `subscribe(listener)` API plus connection status via context.
*Alternative:* let each screen open its own socket — rejected: N sockets per client,
duplicated reconnect logic, and the bug we're fixing is precisely that most screens
don't.

**2. Invalidate, don't patch.** A `useLiveCacheSync` hook (mounted by the provider)
subscribes to events and calls `queryClient.invalidateQueries({ queryKey: [prefix] })`
for the affected prefixes. TanStack refetches only *active* (mounted) queries by default,
so off-screen data is simply marked stale and refetched on next mount — cheap and correct.
*Alternative:* `setQueryData` to splice the new lap into every cached list — rejected:
must replicate server-side sort/filter/paging/dedup per query; brittle.

Event → prefix map:
- `LapCreated` → `laps`, `laps-aggregate`, `session-laps`, `session-best-lap`,
  `track-best-lap`.
- `SessionCreated|SessionStarted|SessionUpdated|SessionEnded` → `sessions`,
  `sessions-aggregate`, `session` (the affected record).

**3. LiveScreen consumes the shared subscription.** Refactor `useLiveEvents` to accept
the shared `subscribe` instead of `new WebSocket(...)`. Its `useState` model, REST
catch-up fetches, optimistic lap prepend, and track-map accumulation are unchanged — only
the event *source* changes.

**4. Gating.** The provider is inert unless `mode === "live"` and `links.live` is present
(same guards `LiveScreen`/`queries.ts` already use). In MOCK mode it opens no socket and
registers no invalidations.

## Risks / Trade-offs

- **Invalidation storm during heavy telemetry** → `PlayerCarUpdated` is high-frequency;
  it is NOT in the invalidation map (telemetry is `LiveScreen`-local state only). Only
  lap/session lifecycle events invalidate, which are low-frequency (~once per lap).
- **Refetch races / flicker** → list hooks already use `placeholderData: (prev) => prev`,
  so refetches keep prior data visible until new data lands; no empty flash.
- **Double-fetch on the Live screen** (its own REST catch-up + a cache invalidation) →
  acceptable; both hit the same endpoint and TanStack dedupes concurrent identical keys.
  LiveScreen's lap list is `useState`, not query-backed, so it isn't even targeted.
- **Reconnect gaps** → events fired while disconnected are missed, same as today; on
  reconnect, the next mount/refetch reconciles. Acceptable for a dashboard.

## Migration Plan

Pure frontend, additive. Ship the provider + hook, then refactor `LiveScreen` to the
shared source in the same change. Rollback = revert the frontend commit; backend
untouched. No data migration.

## Open Questions

- Should a `SessionEnded` also invalidate `laps`/`session-laps` to capture any final lap
  delivered out of order? Default: no (the final lap arrives as its own `LapCreated`);
  revisit if races show a missing last lap.
