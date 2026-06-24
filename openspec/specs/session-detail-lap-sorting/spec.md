# session-detail-lap-sorting

## Purpose

Defines how the Session Detail screen's laps table supports column sorting,
consistent with the Laps and Sessions screens: which headers are sortable,
how sorting is applied server-side via HATEOAS links, how the active sort is
persisted in the URL, and how the laps trend Sparkline remains chronological
independent of the table's sort.

## Requirements

### Requirement: Session Detail laps table is column-sortable

The Session Detail screen's laps table SHALL render its column headers as sortable controls, consistent with the Laps and Sessions screens. A header SHALL be clickable only when its mapped field appears in the `sortable` array advertised on the laps page response (`Page.sortable`); fields absent from that array SHALL render as plain, non-clickable labels.

#### Scenario: Sortable headers reflect advertised fields
- **WHEN** the laps page response for a session advertises `sortable` containing `lapTime`
- **THEN** the laps table's lap-time header is clickable
- **AND** a header whose field is not in `sortable` is rendered as a plain label

#### Scenario: Clicking a header cycles sort order
- **WHEN** the user clicks an unsorted sortable header
- **THEN** the column sorts ascending, the next click sorts descending, and the next click clears the column's sort
- **AND** the active column shows a direction indicator

### Requirement: Sorting is performed server-side via the session laps HATEOAS link

Sorting SHALL be applied by the backend, not in the browser. The screen SHALL request the sorted laps by following the session resource's `_links.laps` HATEOAS rel with a `sort=field:ORDER` query parameter, where `ORDER` is `ASC` or `DESC`. The set of acceptable field names is whatever the laps endpoint advertises in `Page.sortable`.

#### Scenario: Sort change refetches via the laps link
- **WHEN** the user selects a sort of `lapTime:ASC`
- **THEN** the screen fetches laps from the session's `_links.laps` URL with `sort=lapTime:ASC`
- **AND** the table renders the laps in the order returned by the backend

#### Scenario: No explicit sort uses the default
- **WHEN** no sort is set
- **THEN** laps are fetched with the default `sort=lapNumber:ASC`

### Requirement: Active sort is persisted in the URL

The active sort SHALL be written to the page URL querystring as `sort=field:ORDER` so the ordering is restored on reload and is shareable via deep-link, matching the other sortable tables. An absent or malformed `sort` parameter SHALL fall back to the default ordering.

#### Scenario: Sort restored from the URL
- **WHEN** the user opens a Session Detail URL containing `?sort=lapTime:DESC`
- **THEN** the laps table loads sorted by lap time descending with that header marked active

#### Scenario: Clearing sort removes the URL parameter
- **WHEN** the user toggles the active sorted column off
- **THEN** the `sort` parameter is removed from the URL and the default ordering is applied

### Requirement: Laps trend Sparkline stays chronological

The laps trend Sparkline SHALL display valid lap times in chronological (lap-number) order regardless of the table's current sort, because the trend represents a timeline rather than a ranking.

#### Scenario: Sorting the table does not reorder the trend
- **WHEN** the user sorts the laps table by `lapTime`
- **THEN** the Sparkline continues to plot valid lap times in lap-number order
