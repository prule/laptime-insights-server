## Context

Lap and session search share a generic Exposed query layer:

- `utils/.../exposed/QueryExtension.kt` — `Query.paginate` (list) and `Query.firstOrNull` (single). Both resolve sort fields via a `SortableFields.mapping[name] -> Column` lookup, then `ORDER BY`.
- `app/.../persistence/{lap,session}/*Repository.kt` — each defines `SearchCriteria.toQuery()` building the `WHERE`, including the `from`/`to` date range.

Three defects exist today:

1. `firstOrNull` resolves with `mapping[it.field]!!` (line 43) — NPE on any unknown sort field. `paginate` uses `mapNotNull` and silently skips. Same input, two behaviours.
2. `LapSearchCriteria` KDoc and the `to` param docs say `to` is **exclusive**, but `LapRepository.toQuery()` (`recordedAt lessEq it`) and `SessionRepository.toQuery()` (`startedAt lessEq it`) apply it **inclusively**. The Lap repo even has a comment claiming "inclusive upper bound" — contradicting its own KDoc.
3. Unknown sort fields are dropped with no log line — by-design forward-compat, but undiscoverable.

## Goals / Non-Goals

**Goals:**
- One consistent rule for unknown sort fields across single and paginated paths: skip, never throw.
- Date range is a half-open interval `[from, to)`; SQL, param docs, and KDoc all agree.
- Dropped sort fields are observable at DEBUG.

**Non-Goals:**
- No change to which fields are sortable, nor to the `400`-vs-silent-drop policy (still silent — only adds a log).
- No new endpoints, no schema changes, no frontend changes.

## Decisions

- **Align `firstOrNull` onto `paginate`'s `mapNotNull`** rather than the reverse. Skipping unknown fields is the safer, already-shipped behaviour for the paginated list endpoint; making the single-result path throw would be a regression. Alternative (throw `400` in both) rejected: it breaks the documented forward-compat contract and is a wider behaviour change than this debt item warrants.
- **Switch `to` to `less` (exclusive), not the docs to "inclusive".** The half-open interval `[from, to)` is the convention already written in the KDoc and param docs, and it composes cleanly for adjacent time buckets (no double-counting at boundaries). Aligning SQL to docs is the smaller, less surprising move than rewriting the documented contract. `from` stays `greaterEq` (inclusive).
- **Log dropped fields at DEBUG inside the shared extension**, in both `paginate` and `firstOrNull`, so coverage is uniform and lives at the single resolution point. Use the existing logging setup (`logback.xml`); no new dependency.
- **Extract the sort-resolution into one shared helper** used by both functions, so the skip-and-log behaviour cannot drift apart again.

## Risks / Trade-offs

- **`to` becoming exclusive changes results for a row exactly at `to`.** → Low blast radius: documented behaviour was already exclusive, so any correct client already assumes it. Flagged BREAKING in the proposal for the boundary edge case. Mitigation: explicit boundary tests for both lap and session search lock the new behaviour.
- **DEBUG logging of caller-supplied field names.** → Field names are not sensitive (sort keys), and DEBUG is off in production by default. Acceptable.
- **Shared-helper refactor touches the hot search path.** → Behaviour-preserving for valid fields; covered by existing search tests plus the new ones.

## Migration Plan

Pure code fix, no data migration. Deploy with the app. Rollback = revert the commit; no persisted state depends on the change.

## Open Questions

None.
