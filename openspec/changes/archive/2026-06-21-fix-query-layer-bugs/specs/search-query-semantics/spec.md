## ADDED Requirements

### Requirement: Unknown sort fields are skipped, never fatal

The lap and session search query layer SHALL resolve each requested sort field against the resource's allowed sortable fields. Unknown field names SHALL be skipped (forward-compatibility), and skipping a field SHALL NOT raise an error. This behaviour SHALL be identical for single-result lookups (`searchForOne` / `Query.firstOrNull`) and paginated search (`search` / `Query.paginate`).

#### Scenario: Unknown sort field in single-result lookup

- **WHEN** a caller invokes a single-result search with a `sort` field that is not in the resource's sortable-field mapping
- **THEN** the query executes without raising `NullPointerException`
- **AND** the unknown field is omitted from the SQL `ORDER BY`

#### Scenario: Unknown sort field in paginated search

- **WHEN** a caller invokes a paginated search with a `sort` field that is not in the resource's sortable-field mapping
- **THEN** the unknown field is omitted from the SQL `ORDER BY`
- **AND** results are returned ordered by any remaining valid sort fields

#### Scenario: Dropped sort field is logged

- **WHEN** a requested sort field is dropped because it is not in the sortable-field mapping
- **THEN** the system logs the dropped field name at DEBUG level

### Requirement: Date-range filter uses a half-open interval

The `from` and `to` query parameters on lap and session search SHALL constrain the resource timestamp (`LAP.recordedAt` / `SESSION.startedAt`) as a half-open interval `[from, to)`: `from` is inclusive and `to` is exclusive. Both parameters are optional and combined with logical AND. The SQL applied, the parameter documentation, and the criteria KDoc SHALL all agree on this semantics.

#### Scenario: Lower bound is inclusive

- **WHEN** a search specifies `from` equal to a row's timestamp
- **THEN** that row is included in the results

#### Scenario: Upper bound is exclusive

- **WHEN** a search specifies `to` equal to a row's timestamp
- **THEN** that row is excluded from the results

#### Scenario: Row strictly inside the range

- **WHEN** a search specifies `from` and `to`, and a row's timestamp is strictly between them
- **THEN** that row is included in the results
