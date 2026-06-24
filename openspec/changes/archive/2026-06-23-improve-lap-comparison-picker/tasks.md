## 1. URL state & seeding

- [x] 1.1 Change CompareScreen URL state from `{ track, lap1, lap2 }` to `{ track, anchor, challenger }`.
- [x] 1.2 Add a back-compat shim: if `lap1`/`lap2` present and `anchor`/`challenger` absent, map `lap1→anchor`, `lap2→challenger`. (Old links already carry `track`, so no derivation needed.)
- [x] 1.3 Add a latest-session seed (derived, not URL-written; applies only when `track`/`anchor` are absent) that sets `track` from `useSessions({ sort: "startedAt:DESC", size: 1 })` — `useCompareSeed`.
- [x] 1.4 On track change, clear `anchor`/`challenger` (and legacy `lap1`/`lap2`) so they can't carry across tracks; the anchor re-seeds via the resolver.

## 2. Anchor

- [x] 2.1 Implement anchor resolver: player's fastest valid lap in the latest session (via `useSessionLaps`, filtered/sorted client-side), fallback to the session best, then `useTrackBestLap` — in `useCompareSeed`.
- [x] 2.2 Build the anchor control (`AnchorControl`: shows lap time + car + session, "change" modal) replacing the Lap 1 `LapPicker`; explicit selection writes the `anchor` param and bypasses the resolver.
- [x] 2.3 Seed the "Same car" car filter from the anchor's car.

## 3. Challenger leaderboard

- [x] 3.1 Replace the Lap 2 `LapPicker`/modal `LapBrowser` with an inline ranked leaderboard (`LapLeaderboard`) bound to `useLaps` (always `track=axis`, `validLap:true`, `sort:"lapTime:ASC"`, paginated).
- [x] 3.2 Add toggles: `[This session | All sessions]` (session constraint via `useSessionLaps`), `[Me | Field]` (`playerLap`), `[Same car]` default ON (`car`).
- [x] 3.3 Compute rank as `(page-1)*size + indexInPage + 1` (server pages) / position-in-list (this-session) and pass to rows.
- [x] 3.4 Mark the anchor lap row and disable it as a challenger pick (reuse `disabledLapUid`/`isRowDisabled`).
- [x] 3.5 Selecting a row sets the `challenger` URL param.

## 4. Row tagging (LapTable)

- [x] 4.1 Rank rendered via `LapTable.prefixColumn` (no LapTable change needed); the "me" indicator is LapTable's existing player dot (`lap.playerLap`, null = not-me), so LapsScreen is unaffected.
- [x] 4.2 Owning session shown via LapTable's existing Session column.

## 5. Entry points & cleanup

- [x] 5.1 Update SessionDetail "vs best" / "vs PB" / "pick…" and LapsScreen multi-select navigations to the new `{ track, anchor, challenger }` param shape.
- [x] 5.2 Removed `LapPicker.tsx` and `LapBrowser.tsx` (fully replaced).

## 6. Tests & docs

- [x] 6.1 Tests (`CompareScreen.test.tsx`): latest-session seeding, anchor my-fastest→session-best fallback, same-track reset on track change, leaderboard ranking + challenger pick, back-compat `lap1`/`lap2` shim.
- [x] 6.2 Updated `docs/user-guide.md` (Compare workflow) and `docs/frontend-technical.md`.
- [x] 6.3 Ran `pnpm typecheck`, `pnpm lint`, `pnpm test`, `pnpm build` — all pass (0 errors).
