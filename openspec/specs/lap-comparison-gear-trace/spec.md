# lap-comparison-gear-trace

## Purpose

Defines the Compare screen's gear visualization: a continuous, stepped, dual-lap gear trace plotted
against track position, so the user can read what gear each lap is in at any point (not just where
the two laps differ). Mismatch shading and the synchronized crosshair are retained.

## Requirements

### Requirement: Continuous dual-lap gear trace

The Compare screen SHALL show gear as a continuous trace for both compared laps, plotted against
track position (0 → 1) on a single overlaid chart, so the user can read the actual gear of each lap
at any point on track. The two laps SHALL use the same colours as the speed trace (anchor and
challenger) and share the chart's X axis with the other panels.

#### Scenario: Both laps' gears are shown across the lap

- **WHEN** a comparison is rendered for two laps
- **THEN** the gear chart draws one trace per lap across the full track position range
- **AND** each trace uses its lap's colour (anchor vs challenger)

#### Scenario: No gear data

- **WHEN** either lap has no telemetry samples
- **THEN** the gear chart renders nothing rather than an empty or broken axis

### Requirement: Stepped rendering with integer gear axis

Because gear is a discrete integer, the trace SHALL be drawn as a stepped line — each gear value is
held until the next gear change rather than interpolated between samples — and the Y axis SHALL be
scaled to integer gear values across the laps' observed range.

#### Scenario: Gear held until it changes

- **WHEN** consecutive samples report the same gear
- **THEN** the trace is flat across that span
- **AND** a gear change is drawn as a vertical step at the position where it occurs

### Requirement: Mismatch shading preserved

The gear chart SHALL retain the prior "where do the laps differ" insight by shading the track
positions where the two laps are in different gears, drawn behind the traces so both the gear values
and the mismatch are legible at once.

#### Scenario: Differing gears are shaded

- **WHEN** the two laps are in different gears at a track position
- **THEN** that position is shaded behind the traces
- **AND** positions where the gears match are not shaded

### Requirement: Synchronized hover

The gear chart SHALL participate in the Compare screen's synchronized crosshair: hovering it updates
the shared hover position, and a change to the shared position draws the crosshair on the gear chart,
matching the speed and speed-delta panels.

#### Scenario: Hover syncs across panels

- **WHEN** the user hovers over the gear chart
- **THEN** the shared hover position updates and every panel draws its crosshair at that position

#### Scenario: External hover draws the crosshair

- **WHEN** the shared hover position is set by another panel
- **THEN** the gear chart draws its crosshair at that position
