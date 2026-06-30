## 1. Data contract & snapshot

- [x] 1.1 Add `src/api/profile-data.ts` (or `src/data/`) defining typed interfaces: `ProfileData`, `ProfileMeta`, `Profile`, `Totals`, `PerTrack`, `RecordRow`, `Highlight`, and the `HeatmapWeek`/`HeatmapDay` shapes.
- [x] 1.2 Port the bundled `marco-rossi` sample from the handoff `profile-data.js` into a typed `SAMPLE_PROFILE: ProfileData` constant (identical field names).
- [x] 1.3 Port the `TrackArt` SVG path map and the `buildHeatmap()` helper as pure, typed functions.
- [x] 1.4 Add a thin accessor (e.g. `useProfileData()`) returning the bundled snapshot, with a comment that it will later become a data-mode-keyed fetch.

## 2. Feature registration & routing

- [x] 2.1 Add `"public-profile"` to the `Feature` union and `FEATURES` list in `config/feature-types.ts`.
- [x] 2.2 Add a `public-profile` entry to `FEATURE_CONFIG` in `config/features.tsx` with nav label "Public Profile", an icon consistent with the others, and a route to `PublicProfileScreen`.
- [x] 2.3 Make the nav/route statically enabled (not HATEOAS-gated) for now, with a `// TODO: gate via HATEOAS` comment; verify it appears in the sidebar regardless of backend links.

## 3. Components (Driver Passport)

- [x] 3.1 Create `components/profile/LicenseCard.tsx` — identity card (initials, name, tagline, location, member-since, top car, cars count, season label, MRZ flourish).
- [x] 3.2 Create `components/profile/SeasonTotals.tsx` — totals strip (laps, distance, seat time, sessions, active days, circuits).
- [x] 3.3 Create `components/profile/ActivityHeatmap.tsx` — GitHub-style heatmap with month labels, legend, and streak/active-days summary.
- [x] 3.4 Create `components/profile/TrackStamps.tsx` — proportional, ranked per-circuit stamps with lap counts.
- [x] 3.5 Create `components/profile/RecordsLedger.tsx` — records table (circuit, car, season best, all-time best + date, PB marker).
- [x] 3.6 Map handoff `--lt-*` CSS to existing Tailwind tokens; keep only decorative/data-driven flourishes (foil, guilloché, per-track accents) as inline/arbitrary styles.

## 4. Screen assembly

- [x] 4.1 Create `screens/PublicProfileScreen.tsx` composing the components from the snapshot accessor.
- [x] 4.2 Confirm it renders inside the existing `AppShell` and matches the look of other screens.

## 5. Tests

- [x] 5.1 Add a render test for `PublicProfileScreen` (Vitest + Testing Library) asserting key snapshot values appear (name "Marco Rossi", a total, a record row, a track stamp).
- [x] 5.2 Add a unit test for `buildHeatmap()` (week grouping, level thresholds).
- [x] 5.3 Run `pnpm typecheck`, `pnpm lint`, and `pnpm test`; fix issues.

## 6. Docs & cleanup

- [x] 6.1 Add a short note under `docs/` describing the Public Profile page and the `ProfileData` JSON contract.
- [x] 6.2 Record the temporary static-enabled nav (un-gated feature) in `docs/technical-debt.md` with a pointer to the future cloud-publish change.
- [x] 6.3 Update `MEMORY.md` if any non-obvious decision was made during implementation.
