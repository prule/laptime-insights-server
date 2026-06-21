## Why

The shared query layer behind `GET /api/1/laps` and `GET /api/1/sessions` has three latent defects: an unsafe `!!` that NPEs on an unknown sort field, a date-range upper bound whose SQL (`lessEq`, inclusive) contradicts its own documentation (`to` is "exclusive"), and unknown sort fields that vanish with no signal. Each is a correctness or operability bug a client can trip today.

## What Changes

- **Fix sort NPE**: `Query.firstOrNull` resolves sort fields with `sortableFields.mapping[it.field]!!`, throwing `NullPointerException` on any field not in the map. Align it with `Query.paginate`, which uses `mapNotNull` to silently skip unknown fields. After this, `searchForOne` and `search` handle unknown sort fields identically.
- **Fix date-range bound mismatch**: Both `LapSearchCriteria` and the `to` parameter docs state `from` inclusive / `to` exclusive, but `toQuery()` applies `to` with `lessEq` (inclusive) in both `LapRepository` and `SessionRepository`. Align SQL to the documented half-open interval `[from, to)` by switching `to` to `less`. Confirm and document `from` as inclusive (`greaterEq`).
- **Surface dropped sort fields**: Unknown `sort` field names are dropped silently (forward-compat by design). Keep the behaviour but log at DEBUG when a field is dropped, so misuse is discoverable.

## Capabilities

### New Capabilities
- `search-query-semantics`: Defines the cross-cutting query behaviour shared by lap and session search — sort-field resolution (unknown fields skipped, not fatal; logged at DEBUG) and date-range bound semantics (`from` inclusive, `to` exclusive).

### Modified Capabilities
<!-- None: no existing spec covers the search query layer. -->

## Impact

- **Code**:
  - `utils/.../tracker/utils/data/exposed/QueryExtension.kt` — `firstOrNull` `!!` → `mapNotNull`; add DEBUG logging of dropped fields in both `firstOrNull` and `paginate`.
  - `app/.../adapter/out/persistence/lap/LapRepository.kt` — `to` `lessEq` → `less`; fix the "inclusive upper bound" comment.
  - `app/.../adapter/out/persistence/session/SessionRepository.kt` — `to` `lessEq` → `less`.
- **API behaviour**: `to` becomes exclusive on `GET /api/1/laps` and `GET /api/1/sessions`. A lap/session recorded exactly at `to` is no longer returned. Docs already claim exclusive, so this aligns behaviour to the contract rather than breaking it — but any client relying on the inclusive bug must adjust. Mark **BREAKING** for the `to` boundary edge case.
- **Tests**: add coverage for unknown-sort-field handling (no NPE in `searchForOne`) and for `to`-exclusive boundary on both lap and session search.
- **Docs**: `docs/technical-debt.md` items for these three bugs marked DONE.
