# public-profile

## Purpose

The Public Profile capability provides a shareable "Driver Passport" page that
summarises a driver's season at a glance — identity, season totals, activity
heatmap, per-track stamps, and a records ledger — rendered inside the standard
app shell. In this phase the page is driven entirely by a typed profile snapshot
(no backend request), with the type definition shaped so it can later be served
over HTTP unchanged.

## Requirements

### Requirement: Public Profile navigation entry
The frontend SHALL expose a "Public Profile" entry in the left-hand sidebar that routes to the Driver Passport page.

#### Scenario: Sidebar shows Public Profile
- **WHEN** the app shell renders the sidebar
- **THEN** a "Public Profile" nav item is listed alongside the other features
- **AND** clicking it navigates to the Public Profile route without a full page reload

#### Scenario: Direct navigation to the route
- **WHEN** the user loads the Public Profile route URL directly
- **THEN** the Driver Passport page renders inside the app shell

### Requirement: Profile snapshot data contract
The Public Profile page SHALL be driven entirely by a typed profile snapshot, with no backend request in this phase. The snapshot SHALL include profile identity, season meta, season totals, per-track laps, and per-track/car records.

#### Scenario: Snapshot fields are typed and present
- **WHEN** the bundled sample snapshot is loaded
- **THEN** it provides `meta` (slug, season, range, generatedAt, sim), `profile` (name, slug, initials, tagline, location, member_since), `totals`, `perTrack`, `records`, and `highlight`
- **AND** the type definition matches the JSON shape so it can later be served over HTTP unchanged

#### Scenario: Page renders from the snapshot
- **WHEN** the Public Profile page mounts
- **THEN** all displayed values are derived from the snapshot rather than hardcoded in the markup

### Requirement: Driver Passport identity card
The page SHALL render an identity/license card showing the driver's initials, name, tagline, base location, membership year, most-driven car, cars-raced count, and the season label.

#### Scenario: Identity card reflects the snapshot
- **WHEN** the page renders for the sample snapshot
- **THEN** the card shows "Marco Rossi", initials "MR", the tagline, location, member-since year, top car, and the "2025 Season" label

### Requirement: Season totals strip
The page SHALL render a totals strip summarising laps, distance, seat time, sessions, active days, and circuits for the season.

#### Scenario: Totals reflect the snapshot
- **WHEN** the page renders for the sample snapshot
- **THEN** each total cell shows the corresponding value from `totals` with its label

### Requirement: Activity heatmap
The page SHALL render a GitHub-style activity heatmap derived from the snapshot for the season, including a longest-streak and active-days summary.

#### Scenario: Heatmap renders cells per day
- **WHEN** the page renders
- **THEN** the heatmap shows day cells grouped into week columns with intensity levels
- **AND** a summary shows the longest streak and active-days count

### Requirement: Track stamps
The page SHALL render one stamp per circuit in `perTrack`, sized proportionally to laps, ranked, and showing the lap count.

#### Scenario: Stamps reflect per-track laps
- **WHEN** the page renders for the sample snapshot
- **THEN** one stamp appears per circuit, ranked by laps, each showing the circuit name and lap count

### Requirement: Records ledger
The page SHALL render a records table listing, per circuit, the car, season-best time, all-time-best time with its date, and a personal-best indicator.

#### Scenario: Records ledger reflects the snapshot
- **WHEN** the page renders for the sample snapshot
- **THEN** each row shows circuit, car, season best, all-time best with date, and a PB marker when `isPB` is true

### Requirement: Visual consistency with the app shell
The Public Profile page SHALL use the application's existing Tailwind design tokens and render inside the standard app shell, matching the look and feel of other screens.

#### Scenario: Page uses app styling
- **WHEN** the page renders
- **THEN** colors, typography, radii, and surfaces come from the app's design tokens rather than inline styles copied verbatim from the handoff
