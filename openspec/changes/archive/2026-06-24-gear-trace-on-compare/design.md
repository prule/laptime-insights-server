## Context

The Compare screen (`frontend/src/screens/CompareScreen.tsx`) renders four telemetry panels for two
laps, all sharing one X axis (track position 0 → 1) and a lifted `hoveredPosition` crosshair:

- **Speed (KPH)** — `TelemetryTrace`, continuous overlaid polylines per lap.
- **Track map** — `TrackMap`.
- **Speed delta** — `SpeedDeltaTrace`, anchor minus challenger.
- **Gear mismatch** — `GearMismatchStrip`, a thin strip of red blocks where the two laps' gears
  differ. It conveys *where* gears differ but never *what gear* either car is in.

`TelemetrySample.gear` is already present on every sample (`0` = neutral, `-1` = reverse, `1..N` =
forward). `TelemetryTrace` is hard-typed to `field: "speedKph"` and draws an interpolated polyline.

## Goals / Non-Goals

**Goals:**
- Show the actual gear of both laps at every track position, overlaid, like the speed trace.
- Draw gear as a stepped line (discrete integers) with an integer Y axis.
- Preserve the mismatch insight as shading behind the traces.
- Reuse the existing synchronized crosshair/hover contract.

**Non-Goals:**
- No backend/API change — gear is already in the telemetry payload.
- No change to the speed, track-map, or delta panels.
- No new gear analytics (shift counts, optimal-gear suggestions) — just the trace.

## Decisions

### New `GearTrace` component rather than generalising `TelemetryTrace`

Add `frontend/src/components/ui/GearTrace.tsx` instead of widening `TelemetryTrace`'s `field` union.
Gear rendering differs enough — **stepped** path (hold value, vertical step on change) vs
interpolated slope, an **integer** Y axis, and **mismatch shading** — that overloading the speed
component would tangle both. The new component copies `TelemetryTrace`'s proven structure: 1000-wide
viewBox, `preserveAspectRatio="none"`, `hoveredPosition`/`onHover` props, nearest-sample crosshair
tooltip, and per-lap colours passed in via the same `series` shape.

- Alternative considered: add `field: "speedKph" | "gear"` plus a `stepped`/`integerAxis` flag to
  `TelemetryTrace`. Rejected — more branches in a shared component for one extra panel; a focused
  component is clearer and keeps the speed path untouched.

### Stepped path construction

For each lap, sort samples by `splinePosition` and emit an SVG path that, between consecutive
samples, draws a horizontal segment at the current gear then a vertical segment to the next gear
(`H`/`V`-style: `L x_next,y_current` then `L x_next,y_next`). Y maps gear linearly across the
observed integer range `[minGear, maxGear]` (with a 0.5 pad top/bottom so the top/bottom gears
aren't clipped). Render gridlines/labels at each integer gear.

### Mismatch shading reuses the existing resample

Keep the `resampleGear(samples, buckets)` bucketing from `GearMismatchStrip` (move it into
`GearTrace`): resample both laps to N buckets, and for buckets where the gears differ, draw a faint
full-height `rect` behind the traces. This preserves the old "where do they differ" readout inside
the richer chart.

### CompareScreen swap

Replace the "Gear mismatch" `Card` + `GearMismatchStrip` with a "Gear" `Card` + `GearTrace`, passing
the same `series` (anchor cyan, challenger red) and `hoveredPosition`/`onHover` already lifted in the
screen. Delete `GearMismatchStrip.tsx` once unreferenced.

## Risks / Trade-offs

- [Reverse/neutral (`-1`/`0`) widen the axis oddly] → The axis spans the observed min/max gear, so
  reverse only appears if actually used; for a flying lap the range is the forward gears driven.
- [Stepped path with sparse samples could miss a brief shift] → Same sampling as today's strip; the
  trace is at least as faithful as the mismatch strip it replaces.
- [Two overlaid stepped lines can sit exactly on top of each other when gears match] → Acceptable —
  identical gear *should* overlay; the mismatch shading highlights where they diverge.

## Migration Plan

Frontend-only, no data migration. Swap the panel and delete the old component. Rollback = restore
`GearMismatchStrip` and revert the CompareScreen panel.

## Open Questions

- Should the Y axis always include neutral/reverse for a fixed scale, or fit to the observed range?
  Leaning fit-to-observed for readability; revisit if users want a stable axis across comparisons.
