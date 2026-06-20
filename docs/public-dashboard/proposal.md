## Why

LapTimeInsights is self-hosted and private. Users want to *share* their progress publicly
— how much they race and their best times per track/car combination — without exposing
raw telemetry or giving up the local-first model.

This **separate cloud project** provides the public side of that: a place users sign in,
subscribe ($12/year), receive a token they paste into their local app, and get a public
page at `/dashboard/<username>` that renders the aggregated summary their local app
publishes. It is intentionally decoupled from the local app — they share only the
`POST /publish` HTTP contract.

## What Changes

This is a new project. It provides:

- **Sign-in & account**: authenticate (Supabase Auth — Google / email), claim one unique,
  immutable `username` per account.
- **API token issuance**: generate a token shown once, stored hashed. The token is the
  write credential the local app uses to publish. Support **revoke / regenerate** so a
  leaked token can be invalidated.
- **Subscription ($12/year)**: Stripe Checkout → webhook sets the account's `expires_at`.
  Subscription state gates whether publishes are accepted and whether the public page
  renders.
- **Publish receiver**: `POST /publish` authenticated by `Bearer` token. Verifies the
  account and active subscription, then **replaces** the account's stored summary with the
  posted snapshot.
- **Public page**: `GET /dashboard/<username>` renders activity totals + best-lap-per-
  track/car. If the subscription has expired, render a **"subscription expired / paused"**
  page (data retained, restored on renewal). Unknown username → 404.

## Capabilities

### New Capabilities

- `public-dashboard`: Cloud service providing sign-in, a unique username, a revocable
  publish token, a $12/year Stripe subscription gating visibility, a token-authenticated
  full-snapshot `POST /publish` receiver, and a public `/dashboard/<username>` page that
  renders activity totals and best lap per track/car — with an "expired" state when the
  subscription lapses.

## Impact

- New repository. Suggested stack (matches existing Cloudflare usage for `landing/`):
  **Cloudflare Worker** (webhook, `/publish` receiver, public page render) + **Supabase**
  (Auth + Postgres). No dependency on this repo's code.
- Shared contract: `POST /publish` — must stay byte-compatible with the local app's
  `openspec/changes/add-cloud-publish` spec.
- External services: Supabase (auth + DB), Stripe (payments). Both are configuration/
  secrets, not code in the local app.
