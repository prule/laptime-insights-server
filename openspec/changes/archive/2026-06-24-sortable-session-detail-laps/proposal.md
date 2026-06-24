## Why

The Sessions list and Laps search tables are column-sortable, but the **Laps table on the Session Detail screen** is not — it renders `<LapTable>` without `sort` / `onSortChange` / `sortableFields`, so users can't reorder a session's laps by lap time, lap number, status, etc. This is an inconsistency users notice immediately ("the others sort, this one doesn't").

The plumbing already exists end-to-end: the laps endpoint backend-sorts via `Lap.SORTABLE_FIELDS` and advertises the accepted fields in the `Page.sortable` array; `LapTable` already accepts the sort props; `useSessionLaps` already follows the HATEOAS `session._links.laps` rel and takes a `sort` param. Only the wiring on the Session Detail screen is missing.

## What Changes

- Make the Session Detail "Laps" table column-sortable, consistent with the Laps and Sessions screens.
- Drive sorting **server-side** through the HATEOAS `session._links.laps` link: `useSessionLaps` already forwards `sort=field:ORDER`; surface the response's `sortable` array to `LapTable` so the same headers light up as on the Laps screen.
- Persist the active sort in the URL querystring (`sort=field:ORDER`) so deep-links and reloads restore ordering, matching the other tables. Default stays `lapNumber:ASC`.
- Keep the laps **Sparkline trend** in chronological (`lapNumber`) order regardless of the table sort — the trend is a timeline, not a ranking.
- Client-side car filtering, stats, and the car list are order-independent and remain unchanged.

## Capabilities

### New Capabilities
- `session-detail-lap-sorting`: Column-sortable laps table on the Session Detail screen, driven by the backend `sort` parameter via the session's HATEOAS `laps` link and the advertised `Page.sortable` field set, with sort state persisted in the URL.

### Modified Capabilities
<!-- None — no existing spec's requirements change. -->

## Impact

- Frontend only; no backend changes (the laps endpoint already supports `sort` and advertises `sortable`).
- `frontend/src/screens/SessionDetailScreen.tsx`: read/write `sort` URL state, pass `sort` to `useSessionLaps`, pass `sort`/`onSortChange`/`sortableFields` to `<LapTable>`, decouple the Sparkline from table order.
- `frontend/src/api/queries.ts`: `useSessionLaps` returns the page including `sortable` (already does — confirm it's surfaced).
- No API, dependency, or schema changes.
