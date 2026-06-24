## Context

The Compare screen (`frontend/src/screens/CompareScreen.tsx`) currently renders two symmetric
`LapPicker` components. Each opens a modal `LapBrowser` — a flat, independently-filtered lap list
(track / car / valid-only / PB-only, paginated, sorted by lap time). URL state owns `track`, `lap1`,
`lap2`, where `track` is only a soft pre-fill for each picker's filter.

Two problems follow from the symmetric-flat-list design:

1. Track is per-picker, so the two laps can be from different tracks — a meaningless comparison
   the UI does nothing to prevent.
2. The list only supports absolute picks. Users think in semantic terms ("my fastest", "this
   session's best", "the 2nd fastest", "the fastest lap anyone set in the same car"), which the
   flat list cannot express.

Relevant existing building blocks (reuse, don't rebuild):
- `useSessions({ sort: "startedAt:DESC", size: 1 })` → latest session.
- `useSessionBestLap(session)` → session's fastest valid lap.
- `useLaps({ track, car, playerLap, validLap, sort: "lapTime:ASC", page, size })` → the laps query
  used by `LapBrowser`. Supports every filter the challenger needs.
- `LapResource.playerLap: boolean | null` already distinguishes the player's laps from the field —
  no backend change needed for the "me" badge.
- `LapComparisonResource` / `useLapComparison(lap1, lap2)` powers the charts and is unchanged.

## Goals / Non-Goals

**Goals:**
- Make track a single shared comparison axis, enforced same-track-only, reflected in the URL.
- Seed the screen from the latest session (track + car + default anchor) for a zero-click start.
- Replace symmetric pickers with an anchor (semantic default + fallback) and a ranked, same-track
  challenger leaderboard with scope / driver / same-car toggles.
- Show rank + lap time + "me" badge + owning session on each leaderboard row.
- Keep existing entry points working: SessionDetail "vs best" / "vs PB" and shareable URLs.

**Non-Goals:**
- No new backend endpoints. Rank is a client-side concern derived from sorted pagination.
- No change to the comparison charts/telemetry panels or `useLapComparison`.
- No multi-lap (3+) comparison; still exactly two laps (anchor + challenger).
- No persistence of picker state beyond the URL.

## Decisions

### URL shape: `track` + `anchor` + `challenger`

Replace `{ track, lap1, lap2 }` with `{ track, anchor, challenger }`. `track` is now authoritative
(the axis) rather than a soft pre-fill. `anchor`/`challenger` are lap UIDs. The latest-session seed
runs only when `track` and `anchor` are both absent; any explicit param suppresses the seed so
shared links and "vs best"/"vs PB" entry points are honored verbatim.

- Alternative considered: keep `lap1`/`lap2` and derive track from `lap1`. Rejected — track must be
  selectable before any lap is chosen, and a first-class `track` param makes the shared axis and
  same-track invariant explicit and shareable.

Map the existing SessionDetail entry points: "vs best"/"vs PB" set `track` + `anchor` (the row's
lap) + `challenger` (the session-best / track-best lap) instead of `lap1`/`lap2`.

### Anchor: derived default, not a stored choice until overridden

The anchor defaults via a small resolver: player's fastest valid lap in the seeding session →
fallback to the session's best valid lap. Implement by querying the seeding session's laps with
`playerLap: true, validLap: true, sort: "lapTime:ASC", size: 1`; if empty, use `useSessionBestLap`.
Once the user picks explicitly, the `anchor` URL param holds the choice and the resolver is bypassed.

- Alternative considered: always "my all-time PB at the track". Rejected — the user said the common
  case is comparing within the latest session, so the seed should be session-scoped, not all-time.

### Challenger: one ranked leaderboard, toggles over a single laps query

Replace the modal `LapBrowser` with an inline ranked leaderboard bound to `useLaps`:
- Always `track = <axis>`, `validLap: true`, `sort: "lapTime:ASC"`, paginated.
- `[This session | All sessions]` → adds/removes a session constraint (this-session uses the
  seeding session's laps scoped to the track).
- `[Me | Field]` → toggles `playerLap: true` (Me) vs unset (Field = everyone).
- `[Same car]` (default ON) → sets `car = <seed car>`; OFF removes it.

Rank = absolute position in the sorted, filtered result: `(page - 1) * size + indexInPage + 1`.
The "me" badge reads `lap.playerLap`. The anchor row is marked and non-selectable as challenger
(reuse the existing `disabledLapUid` / `isRowDisabled` mechanism in `LapTable`).

- Alternative considered: keep two modal pickers and just add semantic shortcuts. Rejected — the
  user's framing ("navigate and find these laps") wants a browsable ranked surface, and an inline
  leaderboard makes rank and ownership legible without opening each lap.

### Row tagging lives in `LapTable`

Extend `LapTable` with optional rank and "me"-badge columns (additive, off by default) so the
existing Laps screen is unaffected and the leaderboard opts in.

## Risks / Trade-offs

- [Same-car default ON could hide the obvious challenger if the seed car is rare] → The toggle is
  one click to "any car", and the count/empty state nudges the user when the filter is too tight.
- [`playerLap` is nullable — null when `playerCarId` was unknown at lap creation] → Treat null as
  "not me" for the badge and for the `[Me]` filter; the field/any-car path still surfaces the lap.
- [Rank is per-current-query, not a global standing] → Intended: rank communicates position within
  what the user is looking at. Label it as such ("#" within the filtered list), not "world rank".
- [Changing the track must invalidate stale anchor/challenger] → On track change, clear
  anchor/challenger that don't belong to the new track and re-run the anchor resolver.
- [Entry-point regressions ("vs best"/"vs PB")] → Covered by updating those navigations to the new
  param shape and asserting them in tests.

## Migration Plan

Frontend-only; no data migration. Ship behind the existing route (`/compare`). Old shared links
using `lap1`/`lap2` can be honored with a one-time compatibility shim: if `lap1`/`lap2` are present
and `anchor`/`challenger` are not, map `lap1→anchor`, `lap2→challenger`, and derive `track` from
`lap1`. Rollback = revert the screen/component changes; no schema or API surface changed.

## Open Questions

- "This session" scope when the anchor has been changed to a lap from a *different* session than the
  seed: does "This session" follow the anchor's session or stay pinned to the seeding session?
  Leaning: follow the anchor's session, since the anchor is the reference point.
- Should the leaderboard expose `valid-only` as a visible toggle, or always force valid laps?
  Leaning: always valid for ranking; revisit if users want to inspect invalid laps.
