## Why

The Compare screen shows speed as a continuous overlaid trace for both laps, but gear is only
shown as a "mismatch" strip — red blocks where the two laps differ. That tells you *where* gears
differ but not *what gear* either car is in. When analysing a lap you want to read the actual gear
of both cars at every point on track (e.g. "they took turn 7 in 4th, I was in 3rd"), the same way
you read speed.

## What Changes

- Replace the gear **mismatch strip** with a continuous **gear trace**: both laps' gear plotted
  against track position (0 → 1), overlaid on one chart, using the same colours as the speed trace
  (anchor cyan, challenger red) and the same synchronized crosshair/hover.
- Render gear as a **stepped** line (gears are discrete integers — hold each gear until it changes)
  rather than an interpolated slope, with the Y axis as integer gear numbers.
- Keep the existing "where do they differ" insight by shading the track positions where the two
  laps are in different gears behind the traces, so the mismatch information is preserved within the
  richer chart.
- **BREAKING** (UI only): the `GearMismatchStrip` panel is removed from the Compare screen and
  replaced by the new gear trace panel.

## Capabilities

### New Capabilities
- `lap-comparison-gear-trace`: The Compare screen's gear visualization — a continuous, stepped,
  dual-lap gear trace against track position, with synchronized hover and mismatch shading.

### Modified Capabilities
<!-- None: no existing spec covers the Compare screen's telemetry chart panels. -->

## Impact

- **Frontend**: replace `frontend/src/components/ui/GearMismatchStrip.tsx` with a gear-trace
  component (new `GearTrace.tsx`, or generalise `TelemetryTrace` to a stepped integer field).
  `frontend/src/screens/CompareScreen.tsx` swaps the "Gear mismatch" panel for the gear trace.
- **Data**: none — `TelemetrySample.gear` already carries the per-sample gear; no API change.
- **Docs**: update `docs/user-guide.md` (Compare screen panel description) and
  `docs/frontend-technical.md` if it lists the component.
