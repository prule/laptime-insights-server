## 1. GearTrace component

- [x] 1.1 Add `frontend/src/components/ui/GearTrace.tsx` modelled on `TelemetryTrace`: same `series` shape (`{ samples, color, label }[]`), `height`, `hoveredPosition`, `onHover` props, 1000-wide viewBox with `preserveAspectRatio="none"`.
- [x] 1.2 Compute the integer gear range across both laps and map gear → Y with a 0.5 pad; render a horizontal gridline + label per integer gear.
- [x] 1.3 Draw each lap as a stepped path (hold gear, vertical step on change), in its lap colour.
- [x] 1.4 Move `resampleGear` in and shade buckets where the two laps' gears differ, drawn behind the traces.
- [x] 1.5 Add the synchronized crosshair + nearest-sample tooltip (gear value per lap), matching `TelemetryTrace`.
- [x] 1.6 Return null when either lap has no samples.

## 2. CompareScreen integration

- [x] 2.1 Replace the "Gear mismatch" `Card`/`GearMismatchStrip` with a "Gear" `Card`/`GearTrace`, passing the existing `series` and `hoveredPosition`/`onHover`.
- [x] 2.2 Delete `GearMismatchStrip.tsx` and remove its import.

## 3. Tests & docs

- [x] 3.1 Test `GearTrace`: renders a path per lap, mismatch shading appears only where gears differ, and renders nothing when a lap has no samples.
- [x] 3.2 Update `docs/user-guide.md` (Compare screen — replace the "Gear mismatch" bullet with the gear trace) and `docs/frontend-technical.md` if it references `GearMismatchStrip`.
- [x] 3.3 Run `pnpm typecheck`, `pnpm lint`, `pnpm test`, `pnpm build`; verify the gear panel in the browser.
