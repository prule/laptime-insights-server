## ADDED Requirements

### Requirement: Track is the shared comparison axis

The Compare screen SHALL treat track as a single shared axis selected once, and SHALL only allow
the anchor and challenger laps to be drawn from the same track. The track selection MUST be
reflected in the URL so the comparison is shareable and reload-safe.

#### Scenario: Both laps constrained to the selected track

- **WHEN** a track is selected as the comparison axis
- **THEN** both the anchor control and the challenger leaderboard only offer laps recorded at that
  track
- **AND** it is not possible to produce a comparison whose two laps are from different tracks

#### Scenario: Changing the track resets incompatible selections

- **WHEN** the user changes the comparison track while an anchor or challenger from the previous
  track is selected
- **THEN** any selection no longer valid for the new track is cleared
- **AND** the anchor re-seeds to the default for the new track

### Requirement: Seed the comparison from the latest session

On landing with no explicit selection, the Compare screen SHALL seed itself from the user's latest
session, using that session's track as the comparison axis, the session's car as the default car
filter, and the default anchor lap, so the screen is usable without any clicks.

#### Scenario: Fresh landing seeds from latest session

- **WHEN** the Compare screen opens with no track, anchor, or challenger specified
- **THEN** the comparison track is set to the latest session's track
- **AND** the anchor is set to the default anchor lap for that session
- **AND** the challenger "Same car" filter is seeded from the latest session's car

#### Scenario: Explicit selection overrides the seed

- **WHEN** the Compare screen opens with a track and/or anchor already specified (e.g. via a shared
  link or a "vs best" / "vs PB" entry point)
- **THEN** the explicit selection is honored and the latest-session seed is not applied

### Requirement: Anchor lap with my-fastest then session-best fallback

The Compare screen SHALL provide an anchor lap that defaults, in one click, to the player's fastest
lap in the seeding session. When the player has no lap in that session, the anchor SHALL fall back
to that session's best lap (fastest valid lap by any driver) so an anchor is always present. The
anchor MUST remain changeable to any other lap on the comparison track.

#### Scenario: Player has a lap in the session

- **WHEN** the seeding session contains at least one lap recorded by the player
- **THEN** the anchor defaults to the player's fastest valid lap in that session

#### Scenario: Player has no lap in the session

- **WHEN** the seeding session contains no lap recorded by the player
- **THEN** the anchor falls back to the session's fastest valid lap by any driver

#### Scenario: User changes the anchor

- **WHEN** the user selects a different lap for the anchor
- **THEN** that lap becomes the anchor
- **AND** the selected lap is on the current comparison track

### Requirement: Ranked same-track challenger leaderboard

The Compare screen SHALL present the challenger as a ranked leaderboard of laps on the comparison
track, ordered fastest first. The leaderboard SHALL offer toggles for scope `[This session | All
sessions]`, driver `[Me | Field]`, and `[Same car]` (default ON), and selecting a row SHALL set it
as the challenger lap.

#### Scenario: Leaderboard ordered fastest first

- **WHEN** the challenger leaderboard is shown for the comparison track
- **THEN** laps are listed in ascending lap-time order (fastest first)

#### Scenario: Same-car filter on by default

- **WHEN** the challenger leaderboard first appears
- **THEN** the "Same car" filter is ON, restricting laps to the same car as the seed
- **AND** the user can turn it off to see laps from any car

#### Scenario: Scope toggle narrows to the session

- **WHEN** the user sets the scope toggle to "This session"
- **THEN** the leaderboard lists only laps from the seeding session on the comparison track

#### Scenario: Driver toggle filters to the player

- **WHEN** the user sets the driver toggle to "Me"
- **THEN** the leaderboard lists only laps recorded by the player

#### Scenario: Selecting a challenger row

- **WHEN** the user clicks a leaderboard row
- **THEN** that lap becomes the challenger lap in the comparison
- **AND** the URL updates so the comparison is shareable

### Requirement: Legible leaderboard rows

Each challenger leaderboard row SHALL show the lap's rank within the current filtered list, the lap
time, a badge marking whether the lap is the player's own lap, and the owning session, so the user
can navigate and identify laps without opening each one.

#### Scenario: Row shows rank, time, ownership, and session

- **WHEN** a leaderboard row is rendered
- **THEN** it displays the lap's rank in the current list, the formatted lap time, a "me" badge when
  the lap belongs to the player, and an identifier for the owning session

#### Scenario: Anchor lap is marked in the leaderboard

- **WHEN** the leaderboard includes the lap currently chosen as the anchor
- **THEN** that row is visually marked as the anchor and cannot be selected as the challenger
