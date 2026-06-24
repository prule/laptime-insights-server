## Why

The Compare screen pairs two independent flat lap lists. Each picker re-filters by track/car
from scratch, so the user can pick two laps from *different* tracks (a meaningless comparison),
and the list can only express absolute picks ("this row") — never the semantic picks users
actually reach for: "my fastest here", "the 2nd fastest", "this session's best", "the fastest
lap anyone set in the same car". Finding the right pair is slow and error-prone.

## What Changes

- Make **track the comparison axis**: it is chosen once and shared by both laps. Same-track-only
  is enforced, removing the cross-track mismatch footgun. **BREAKING** for the Compare screen's
  URL/UX shape (two independent track filters collapse into one shared axis).
- **Seed from the latest session** on landing: the latest session supplies the track, the car,
  and the default anchor lap, so the screen is useful with zero clicks.
- Adopt an **anchor + challenger** model instead of two symmetric pickers:
  - **Anchor**: a one-click semantic default = *my fastest lap in the latest session*, with a
    fallback to *the session's best lap* (anyone) when the user has no lap in that session, so it
    always lands on something. Changeable.
  - **Challenger**: a ranked, same-track leaderboard the user sweeps, with toggles
    `[This session | All sessions]`, `[Me | Field]`, and `[Same car]` (default **ON**).
- **Tag list rows** with rank, lap time, a *me*/field badge, and the owning session, so the
  ranked list is legible for navigation.
- The pick vocabulary `{me | field} × {track | session} × {same-car | any-car} × rank` is expressed
  through anchor seeding + challenger toggles; the user's stories ("my fastest vs the session's
  fastest", "my fastest vs my 2nd fastest at this track") become specific toggle states.

## Capabilities

### New Capabilities
- `lap-comparison-picker`: How a user selects the two laps to compare — the shared track axis,
  latest-session seeding, the anchor (with its my-fastest → session-best fallback), and the
  ranked same-track challenger leaderboard with its scope/driver/car toggles and row tagging.

### Modified Capabilities
<!-- None: no existing capability spec covers lap selection on the Compare screen. -->

## Impact

- **Frontend**: `frontend/src/screens/CompareScreen.tsx` (state/URL shape, anchor+challenger
  layout), `frontend/src/components/LapPicker.tsx` and `LapBrowser.tsx` (replaced/reshaped into
  anchor control + challenger leaderboard), `LapTable.tsx` (rank + me/field badge columns).
  Entry points in `SessionDetailScreen.tsx` ("vs best"/"vs PB") must keep working against the new
  shared-track URL shape.
- **API hooks** (`frontend/src/api/queries.ts`): composes existing hooks — latest session via
  `useSessions(sort=startedAt:DESC, size=1)`, `useSessionBestLap`, and the laps query
  (`track`/`car`/`playerLap`/`sort=lapTime:ASC`/paginate). New requirement: the laps list must
  expose, per row, the lap's rank within the current query and whether it is the player's lap.
- **Backend**: likely no new endpoints; the laps query already supports the needed filters/sort.
  Rank is derivable from sorted pagination; the *is-player* flag must be present on the lap
  resource (verify during design).
- **Docs**: update `docs/user-guide.md` (Compare workflow) and `docs/frontend-technical.md`.
