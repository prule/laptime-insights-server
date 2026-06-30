## Context

The frontend is a React 19 / Vite / Tailwind 4 SPA. Navigation is feature-driven: `config/feature-types.ts` lists a `Feature` union, and `config/features.tsx` maps each feature to a sidebar nav entry and React Router routes. Today every feature is gated by a HATEOAS link relation from `GET /api/1` — when the backend omits the rel, the feature's nav and routes are hidden (`FeaturesProvider` + `App.tsx`).

The design handoff (`Laptime insights-public-profile-handoff.zip`) ships the **Driver Passport** concept as a standalone `Passport.html` plus a `profile-data.js` snapshot. It is vanilla HTML/CSS using `--lt-*` CSS variables and an inline `<script>` that imperatively builds the DOM from `window.ProfileData`. The snapshot is explicitly described as "the JSON a paying user's local installation POSTs on a schedule" — one season's aggregates plus all-time bests.

This change ports that concept into the app as a real screen, driven by a typed snapshot, with no backend dependency yet. A later change ([add-cloud-publish]) will add the server-side publish/host flow.

## Goals / Non-Goals

**Goals:**
- A navigable Public Profile (Driver Passport) screen in the sidebar.
- Page rendered declaratively from a single typed `ProfileData` snapshot.
- Reuse the app's Tailwind tokens; visually consistent with existing screens.
- Snapshot type shaped to match the future HTTP payload, so the data source can swap from a bundled file to a fetch without component changes.

**Non-Goals:**
- No backend endpoint, persistence, POST flow, subscription/billing, or HATEOAS gating in this phase.
- No "Season Wrapped" concept (chosen: Passport only).
- No multi-user / vanity-URL routing — single bundled snapshot for now.
- No `.html`/inline-`<script>` ports; rewritten as React components.

## Decisions

### D1: Register as a feature nav entry, statically enabled
Add `public-profile` to the `Feature` union, `FEATURES` list, and `FEATURE_CONFIG`, so the sidebar and router pick it up through the existing registry — no new nav plumbing.

Because there is no backend `public-profile` HATEOAS rel yet, the feature would be hidden if gated normally. Decision: render its nav/route regardless of backend links for now (e.g. treat it as always-enabled in the enabled-features filter), and add a `// TODO: gate via HATEOAS once backend serves the link` note. Alternatives: (a) emit a fake rel — rejected, pollutes the HATEOAS contract; (b) bypass the registry entirely with a hardcoded route — rejected, diverges from the established pattern. Record the temporary static-enable in `docs/technical-debt.md`.

### D2: Typed snapshot module as the data source
Port `profile-data.js` into a typed TS module: a `ProfileData` interface (+ `PerTrack`, `RecordRow`, etc.), the bundled `marco-rossi` sample as a typed constant, plus `buildHeatmap()` and the track-art path map. The screen reads from this module via a thin accessor (e.g. `useProfileData()` returning the bundled snapshot) so the source can later become a TanStack Query fetch keyed by data mode, mirroring the existing MOCK/LIVE pattern.

Alternative: keep raw JS and import untyped — rejected; loses type safety and the contract value the proposal calls for.

### D3: Decompose Passport into presentational components
Break the single HTML file into focused, prop-driven components: `LicenseCard`, `SeasonTotals`, `ActivityHeatmap`, `TrackStamps`, `RecordsLedger`, composed by `PublicProfileScreen`. Each takes its slice of `ProfileData` as props (pure, easy to test). The imperative `innerHTML` builders become JSX `.map()`s.

### D4: Port styling to Tailwind tokens
Map handoff `--lt-*` variables to existing app tokens (`bg`, `surface`, `border`, `accent`, `text-muted`, etc. — already used in `Sidebar.tsx`). Track accent colors and the holographic/foil/guilloché flourishes that have no token equivalent stay as inline `style`/arbitrary Tailwind values driven by snapshot data (e.g. per-track `accent`). This keeps the page consistent with the shell while preserving the distinctive passport look.

### D5: Heatmap derivation stays client-side
Keep `buildHeatmap()` as a pure helper over the snapshot date range. It is deterministic given the snapshot; no need to precompute server-side in this phase.

## Risks / Trade-offs

- [Static-enabled nav diverges from the HATEOAS-gated pattern] → Localize to one filter branch, comment it, and log in `docs/technical-debt.md`; [add-cloud-publish] removes it when the backend serves the rel.
- [Snapshot shape drifts from the eventual server payload] → Treat the TS `ProfileData` interface as the contract of record now so the backend later conforms to it; keep field names identical to the handoff JSON.
- [Inline-style flourishes reduce token consistency] → Acceptable; confined to decorative passport chrome, not layout/semantic colors.
- [Heatmap uses a seeded pseudo-random generator in the sample] → Fine for the bundled demo snapshot; real per-day lap data will replace it when served.

## Migration Plan

Additive frontend-only change; no data migration. Deploy with the normal frontend build. Rollback = revert the change (removes the nav entry and screen). No backend or schema impact.

## Open Questions

- Route path: `/profile` vs `/public-profile`? (Lean `/profile`; confirm at apply time.) Resolved-enough to proceed.
- Should the bundled sample live under `src/` or be loaded like other mock data? Default: a typed constant in the data module, consistent with D2.
