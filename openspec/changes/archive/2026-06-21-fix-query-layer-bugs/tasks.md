## 1. Sort-field resolution (utils)

- [x] 1.1 In `utils/.../exposed/QueryExtension.kt`, extract a shared private helper that maps `Sort` + `SortableFields` to the `ORDER BY` array using `mapNotNull` (skip unknown fields).
- [x] 1.2 In the helper, log each dropped (unknown) sort field name at DEBUG via an SLF4J logger.
- [x] 1.3 Rewrite `firstOrNull` to use the shared helper, removing the `mapping[it.field]!!` NPE.
- [x] 1.4 Rewrite `paginate` to use the same shared helper.

## 2. Date-range bounds (persistence)

- [x] 2.1 In `LapRepository.toQuery()`, change `recordedAt lessEq it` to `recordedAt less it`; fix the "inclusive upper bound" comment to "inclusive lower / exclusive upper".
- [x] 2.2 In `SessionRepository.toQuery()`, change `startedAt lessEq it` to `startedAt less it`.
- [x] 2.3 Confirm `from` stays `greaterEq` (inclusive) in both; update the `SessionSearchCriteria` KDoc to match `LapSearchCriteria`'s `[from, to)` wording (add it if absent).

## 3. Tests

- [x] 3.1 Add a utils/app test: `searchForOne` (single-result) with an unknown sort field returns without throwing and ignores the field.
- [x] 3.2 Add a search test: paginated search with an unknown sort field skips it and orders by remaining valid fields.
- [x] 3.3 Add lap-search boundary tests: row at `from` included, row at `to` excluded, row strictly inside included.
- [x] 3.4 Add session-search boundary tests mirroring 3.3 on `startedAt`.

## 4. Docs

- [x] 4.1 In `docs/technical-debt.md`, mark the three items (firstOrNull/paginate mismatch, `to` inclusive/exclusive, silent dropped sort) DONE with a one-line note each.
- [x] 4.2 Verify the OpenAPI/param docs for `from`/`to` on `GET /api/1/laps` and `GET /api/1/sessions` read inclusive/exclusive; adjust if needed.

## 5. Verify

- [x] 5.1 Run `./gradlew :app:build :app:test` (and `:utils:test` if present) — all green.
