## Why

LapTimeInsights is self-hosted: telemetry is recorded locally and never leaves the
user's network. That privacy is a feature, but it means a user has no way to *share*
their progress — how much they race, their best times per track/car combination — with
anyone outside their LAN.

This change adds an **opt-in, paid, one-way publish** capability to the local app. The
user pairs their local server with a cloud account (a separate project — the public
dashboard) using a token, then publishes an **aggregated summary** of their activity to
that cloud service. The cloud service renders it at a public URL.

The self-hosted promise is preserved deliberately:

- Publishing is **opt-in** and off by default. A fresh install behaves exactly as today.
- The sync is **one-way** (local → cloud). The local H2 database remains the single
  source of truth; the local app never reads cloud data back.
- Only **aggregated summary data** leaves the network — counts and best lap times. No
  raw telemetry samples, no per-tick `RealtimeCarUpdate` rows, no lap traces.

This repo (the downloadable local app) owns everything **up to and including** the
outbound `POST /publish` request. The cloud service that receives that request, handles
sign-in/payment, and renders the public page is a **separate project** — see
`docs/public-dashboard/` for its seed proposal. The shared HTTP contract in
`specs/cloud-publish/spec.md` is the seam between the two.

## What Changes

- Add a **publish configuration**: a cloud base URL, an API token, and a publish
  interval (`off | 15m | 1h | daily`). Off by default; configured via environment
  variables / config file. The token is the trust anchor that identifies the account to
  the cloud service.
- Add a **summary aggregation** in the application layer that computes, from the local
  Session and Lap tables only:
  - **Activity totals**: session count, lap count, total distance, total driving time.
  - **Best lap per (track, car)**: `MIN(lap_time)` over *valid* laps, grouped by track
    and car.
- Add a **publish use case** that builds the summary snapshot and sends it as a
  full-snapshot-replace `POST /publish` to the configured cloud URL with the token as a
  `Bearer` credential. Manual and periodic triggers share this one code path.
- Add a **manual "Publish now" trigger** exposed on the API (and surfaced as a button in
  the frontend) plus a **configurable periodic scheduler** that calls the same use case.
- Surface **publish status** (last published time, last result, whether configured) so
  the frontend can show state and the manual button can report success/failure.
- Update docs: architecture (new optional cloud-publish boundary), user guide (how to
  pair and publish), and `docs/public-dashboard/` for the companion project.

Non-goals (explicitly out of scope for this repo):

- Sign-in, account creation, username allocation, token issuance — cloud project.
- Stripe / payment / subscription expiry — cloud project.
- Receiving `/publish`, storing snapshots, rendering `/dashboard/<username>` — cloud project.

## Capabilities

### New Capabilities

- `cloud-publish`: Opt-in, one-way publish of aggregated activity summary (totals +
  best lap per track/car) from the local app to a paired cloud account, via a
  token-authenticated full-snapshot `POST /publish`, triggered manually or on a
  configurable interval. Off by default; preserves the self-hosted, local-source-of-truth
  model.

### Modified Capabilities

<!-- None. This is additive and opt-in; existing local behaviour is unchanged when
     publishing is not configured. -->

## Impact

- `app/` domain: a `PublishSummary` model (totals + bests) and a port to read aggregates.
- `app/` application: aggregation service + publish use case.
- `app/` adapter/out: an HTTP client adapter that performs the outbound `POST /publish`;
  Exposed read queries for the aggregates.
- `app/` adapter/in/web: a route to trigger a manual publish and expose publish status.
- `app/` infrastructure: publish configuration (env/config), the periodic scheduler,
  manual DI wiring in `AppModule.kt`.
- `frontend/`: a "Publish now" control + publish status display, gated by an
  `enabledFeatures` flag and/or `_links` per the HATEOAS convention (capability link
  present only when publishing is configured).
- Docs: `docs/architecture.md`, `docs/user-guide.md`, and the companion
  `docs/public-dashboard/` seed bundle.
- New runtime dependency: an HTTP client (Ktor client) for the outbound call. No new
  inbound surface beyond the trigger/status route. No DB schema change (reads only).
