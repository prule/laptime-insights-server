## 1. Wire sort state into Session Detail

- [x] 1.1 In `SessionDetailScreen.tsx`, import `useUrlState` + `getString` and the shared `parseSortParam`/`formatSortParam`/`SortState` from `components/ui/SortableHeader`.
- [x] 1.2 Read sort from the URL: `const sort = parseSortParam(getString(params, "sort")) ?? { field: "lapNumber", order: "ASC" }`.
- [x] 1.3 Add `setSort` that calls `setMany({ sort: formatSortParam(next) })` (clearing → `undefined`).
- [x] 1.4 Pass `{ sort: formatSortParam(sort) }` into `useSessionLaps(session, …)` so the laps refetch via the HATEOAS `_links.laps` link with the chosen order.

## 2. Make the laps table sortable

- [x] 2.1 Pass `sortableFields={lapsQuery.data?.sortable}`, `sort={sort}`, and `onSortChange={setSort}` to the `<LapTable>` on the Session Detail screen.
- [x] 2.2 Confirm `useSessionLaps`'s query value is the full `Page<LapResource>` (so `lapsQuery.data.sortable` is available); adjust if it returns only `items`.

## 3. Keep the trend chronological

- [x] 3.1 Compute the Sparkline series from a lap-number-ordered copy of the valid laps (independent of table sort) so the trend stays a timeline.

## 4. Verify

- [x] 4.1 Clicking a sortable header cycles ASC → DESC → cleared and reorders rows via a backend refetch (`sort=field:ORDER` on the laps URL).
- [x] 4.2 Non-advertised columns render as plain labels (driven by `Page.sortable`).
- [x] 4.3 `?sort=lapTime:DESC` deep-link restores sorted order on reload; clearing removes the param.
- [x] 4.4 Sparkline order unchanged when the table is sorted; car filter + stats still correct.
- [x] 4.5 Run frontend lint/tests (`pnpm test` / `pnpm lint` in `frontend/`).
