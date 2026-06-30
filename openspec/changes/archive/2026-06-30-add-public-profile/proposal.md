## Why

Subscribers want a premium, shareable page that shows off a season of ACC driving — a "vanity URL" they can post and link to. A design handoff delivered the *Driver Passport* concept (a credential-style identity card with season totals, an activity heatmap, track stamps, and a records ledger). This change brings that concept into the React app as a real, navigable page driven by a profile JSON snapshot — the same shape a paying user's local install will later POST to the server for public hosting.

## What Changes

- Add a new **Public Profile** screen (Driver Passport concept) to the frontend, reachable from the left-hand sidebar nav.
- Drive the page entirely from a single **profile snapshot JSON** (identity, season totals, per-track laps, records, highlight) plus derived helpers (track art, activity heatmap) — no backend call required in this phase.
- Port the handoff's vanilla-CSS markup to the app's stack: React 19 components + Tailwind CSS 4 semantic tokens, matching the existing AppShell look and feel.
- Define a typed `ProfileData` contract so the JSON shape is stable and ready to be served over HTTP later.
- Ship the bundled sample snapshot (`marco-rossi`) as the page's data source for now, so the feature renders without a backend.
- Keep documentation (`docs/`), sample code, and tests updated per project conventions.
- Note (non-goal here, see Impact): this nav entry is statically enabled rather than HATEOAS-gated, because there is no backend `public-profile` link yet.

## Capabilities

### New Capabilities
- `public-profile`: A subscriber-facing Driver Passport page in the frontend, rendered from a typed profile snapshot JSON, surfaced in the sidebar nav, and structured so the same JSON can later be fetched from / posted to the server.

### Modified Capabilities
<!-- None: no existing spec's requirements change. The sidebar gains an entry but its
     contract (feature-driven nav) is unchanged; this capability slots into it. -->

## Impact

- **Frontend** (`frontend/src/`):
  - New screen `screens/PublicProfileScreen.tsx` and supporting presentational components (license/identity card, totals strip, heatmap, track stamps, records ledger).
  - New data module: typed `ProfileData` contract + bundled sample snapshot + `buildHeatmap`/track-art helpers (ported from `profile-data.js`).
  - `config/features.tsx` + `config/feature-types.ts`: register the `public-profile` nav entry and route.
  - Styling via existing Tailwind tokens; map handoff CSS variables (`--lt-*`) to app tokens.
- **Docs**: add a short note under `docs/` describing the Public Profile page and the profile JSON contract; record any follow-up cleanup in `docs/technical-debt.md`.
- **Tests**: component/render tests for the new screen following existing Vitest patterns.
- **No backend changes** in this phase. A later change ([add-cloud-publish]) will add the POST/host-publicly flow and gate the feature via HATEOAS.
- **Dependencies**: none new; uses React 19, Tailwind 4, React Router 7 already present.
