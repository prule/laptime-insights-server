## Context

Three lap/session tables exist in the frontend:

- **Laps screen** (`LapsScreen.tsx`) — fully sortable: reads `sort` from URL, passes `sort`/`onSortChange`/`sortableFields` (from `lapsQuery.data.sortable`) to `<LapTable>`.
- **Sessions list** (`SessionsScreen.tsx`) — fully sortable via `SortableHeader` + `sessionsQuery.data.sortable`.
- **Session Detail laps** (`SessionDetailScreen.tsx`) — **not** sortable: renders `<LapTable laps={visibleLaps} …>` with no sort props.

The supporting machinery is already in place:

- Backend `GET /api/1/laps` parses `sort` (`request.toSort()`), applies it server-side via `SessionEntity`/`LapEntity.sortableFields`, and advertises accepted fields with `.withSortable(Lap.SORTABLE_FIELDS)` → `Page.sortable`.
- `LapTable` already accepts `sort`, `onSortChange`, `sortableFields` and wires them to `SortableHeader`. Omitting them disables header sorting (used by picker/embedded contexts).
- `useSessionLaps(session, { sort })` already follows the HATEOAS `session._links.laps` rel and forwards `sort`. Its `queryFn` returns the full `Page<LapResource>`, which includes `sortable`.

So this change is pure frontend wiring on the Session Detail screen, plus one ordering nuance for the Sparkline.

## Goals / Non-Goals

**Goals:**
- Session Detail laps table sorts by the same backend-advertised fields as the Laps screen.
- Sorting is server-side, requested through the session's HATEOAS `laps` link.
- Sort state persists in the URL (`sort=field:ORDER`), restored on reload / shareable.
- Keep the laps trend Sparkline chronological regardless of table sort.

**Non-Goals:**
- No backend changes — `sort` and `sortable` already work for the laps endpoint.
- No change to client-side car filtering, stats, or the car-selector list.
- No new sort UI component — reuse `SortableHeader` via `LapTable`'s existing props.

## Decisions

### Decision: Server-side sort via the HATEOAS laps link (not client-side)
Drive sorting through `useSessionLaps(session, { sort })`, which appends `sort=field:ORDER` to `session._links.laps`. The backend returns rows already ordered and re-advertises `sortable` on the page.

*Why over client-side:* consistency with the Laps and Sessions screens (same code path, same field names, same "sortable comes from `Page.sortable`" contract), and it honours the user's note to use the HATEOAS link and the backend sort. Client-side sorting would diverge from the established pattern and duplicate ordering logic. The single round-trip cost is negligible (the screen already refetches on car-filter-independent state and page size is 200).

### Decision: Surface `sortable` from `useSessionLaps`
`useSessionLaps`'s query already returns the whole page. Pass `lapsQuery.data?.sortable` into `<LapTable sortableFields={…}>`. No hook signature change needed beyond confirming the page (with `sortable`) is the query value.

### Decision: Sort state lives in the URL, default `lapNumber:ASC`
Mirror `LapsScreen`: parse `sort` from the querystring with the shared `parseSortParam`, default to `{ field: "lapNumber", order: "ASC" }` (the current implicit order), and `setMany({ sort: formatSortParam(next) })` on change. This keeps deep-links reload-safe and consistent with the other tables.

*Alternative considered:* component state. Rejected — the other tables use URL state and the spec requires shareable/restored ordering.

### Decision: Decouple the Sparkline from table order
The trend Sparkline currently maps over `visibleLaps`, which would reorder once the table sort changes. Compute the Sparkline series from a lap-number-ordered copy of the valid laps so the trend stays a timeline. The table itself uses the backend-sorted order.

*Why:* a "trend" plotted in lap-time-sorted order is meaningless. Cheap to fix with a local sorted copy.

### Decision: carIds / stats unaffected
`carIds` is derived from a `Set` then explicitly sorted (player-first); `stats` are order-independent aggregates. Both already ignore incoming row order, so no change.

## Risks / Trade-offs

- **Car filter + server sort interaction** → The screen fetches up to `size: 200` laps for the session and filters by car client-side. Server sort orders the full set before the client car filter; the filtered subset preserves that order, so sorting remains correct within a car. No mitigation needed.
- **Refetch on every sort click** → Each header click triggers a new `useSessionLaps` fetch. Acceptable: same behaviour as the Laps screen, response is small and cached by React Query keyed on the href (which includes `sort`).
- **Default order regression** → Default must remain `lapNumber:ASC` to preserve today's lap ordering; covered by the spec's default scenario.

## Open Questions

- None blocking. (If product later wants per-car server-side filtering, the `carId` param already exists on the laps endpoint — out of scope here.)
