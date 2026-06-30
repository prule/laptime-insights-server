## ADDED Requirements

### Requirement: Profile snapshot generated from local data

The backend SHALL generate the public profile snapshot from the local Session and Lap data,
merged with signup identity from configuration. The snapshot SHALL conform to the `ProfileData`
contract the frontend consumes (meta, profile, totals, perTrack, records). It MUST NOT include raw
telemetry samples or per-tick realtime car updates. The generated snapshot SHALL be the same
artifact uploaded when cloud publishing is enabled.

#### Scenario: Totals derived from local data
- **WHEN** a snapshot is built
- **THEN** totals (laps, sessions, active days, tracks, cars, most-driven car) are computed from Session and Lap rows
- **AND** identity fields (name, slug, tagline, location, member-since) come from configuration

#### Scenario: Per-track laps and records
- **WHEN** a snapshot is built
- **THEN** it includes laps per track and, per (track, car), the season-best and all-time-best lap time over valid laps
- **AND** invalid laps are excluded from best-lap calculations

#### Scenario: No raw telemetry in the snapshot
- **WHEN** a snapshot is built
- **THEN** no raw telemetry samples or realtime car update rows are included

### Requirement: Profile snapshot served over HTTP

The backend SHALL serve the generated snapshot as JSON from a stable endpoint advertised via the
index resource's `_links`, so the frontend follows the link rather than hardcoding the URL.

#### Scenario: Snapshot endpoint returns the contract
- **WHEN** the public profile is enabled and the snapshot endpoint is requested
- **THEN** it responds with the `ProfileData` JSON generated from local data

#### Scenario: Snapshot link present only when enabled
- **WHEN** the public profile is enabled
- **THEN** the index resource `_links` advertises the snapshot data link
- **WHEN** the public profile is disabled
- **THEN** the snapshot data link is absent

### Requirement: Runtime on/off toggle persisted in config

The backend SHALL allow the public profile to be turned on or off at runtime, defaulting to on. The
chosen state SHALL be persisted to the app's JSON configuration file so it survives a restart.

#### Scenario: Default on
- **WHEN** the app starts with no prior public-profile state configured
- **THEN** the public profile is enabled

#### Scenario: Toggle persists across restart
- **WHEN** the public profile is toggled off
- **THEN** the new state is written to the configuration file
- **AND** the public profile remains off after the app restarts

### Requirement: Toggle exposed as a HATEOAS action

The backend SHALL expose the toggle as an action link in the index resource (`GET /api/1`) and
SHALL gate the `public-profile` feature the standard way: its capability `_link` and
`enabledFeatures` flag are present only when the profile is enabled.

#### Scenario: Toggle action advertised
- **WHEN** the index resource is requested
- **THEN** it advertises a profile-toggle action link the frontend can invoke to change the state

#### Scenario: Feature gated by enabled state
- **WHEN** the public profile is enabled
- **THEN** `enabledFeatures` includes `public-profile` and its capability link is present
- **WHEN** the public profile is disabled
- **THEN** `enabledFeatures` omits `public-profile` and its capability link is absent

### Requirement: Frontend consumes the profile dynamically

The frontend SHALL render the Public Profile page from the snapshot fetched via the index `_links`
(keyed by data mode), not from a bundled constant, and SHALL show loading and error states. The
page nav/route SHALL be gated by the HATEOAS `enabledFeatures` flag with no static override.

#### Scenario: Page renders fetched snapshot
- **WHEN** the public profile is enabled and the page is opened
- **THEN** the page renders values from the fetched snapshot
- **AND** a loading state shows while the fetch is in flight and an error state on failure

#### Scenario: No static enable
- **WHEN** the backend reports the public profile disabled
- **THEN** the sidebar nav item and route are hidden, driven solely by `enabledFeatures`

#### Scenario: User toggles from the UI
- **WHEN** the user invokes the toggle control
- **THEN** the frontend calls the advertised toggle action
- **AND** the profile's availability updates to reflect the new state
