# Design — public-dashboard (cloud project)

## Context

New cloud project. Receives published summaries from many independent self-hosted local
apps, gates them behind a paid subscription, and renders public pages. Adds external
dependencies (Supabase, Stripe). The only coupling to the local app is the `POST /publish`
contract.

## Topology

```
  ┌─ user's browser ─┐     ┌──────────── Cloudflare Worker ───────────┐     ┌── Supabase ──┐
  │ sign in / pay     │────▶│ auth callback, Stripe checkout/webhook    │────▶│ Postgres     │
  │ copy token        │     │ /publish receiver (Bearer)                │     │ + Auth       │
  └───────────────────┘     │ /dashboard/<username> render              │◀────│              │
                            └────────────────────────────────────────────┘     └──────────────┘
        local app  ──POST /publish (Bearer token)──▶ Worker
        visitor    ──GET /dashboard/<username>────▶ Worker
```

Worker chosen to match the existing `landing/` Cloudflare deployment. Supabase gives Auth
+ Postgres + Row Level Security out of the box.

## The shared contract (must match the local app spec exactly)

```
POST /publish
Authorization: Bearer <token>
Content-Type: application/json

{
  "publishedAt": "2026-06-18T09:30:00Z",
  "totals": { "sessions": 142, "laps": 3580, "distanceKm": 18240.5, "drivingTimeSeconds": 345600 },
  "bests":  [ { "track": "spa", "car": "ferrari-296-gt3", "bestLapMs": 138420 } ]
}
```

Receiver behaviour: hash the bearer token → look up account → reject if no match (401) or
subscription expired (402/403). On success, **replace** the account's `activity_totals`
row and **replace** its `best_lap` set wholesale (snapshot semantics). Return 2xx.

## Schema (Postgres)

```
account          ( id, auth_user_id, username UNIQUE, token_hash, expires_at, created_at )
activity_totals  ( account_id PK→account, sessions, laps, distance_km,
                   driving_time_seconds, published_at )
best_lap         ( account_id→account, track, car, best_lap_ms,
                   PRIMARY KEY (account_id, track, car) )
```

- `username` unique + immutable once claimed (prevents squatting/confusion).
- `token_hash` only — the raw token is shown once at issuance, never stored.
- `expires_at` is the single source of subscription truth, set by the Stripe webhook.
- RLS: accounts read/write only their own rows; the public page reads via a server-side
  (service-role) query that filters on `username` + `expires_at > now()`.

## Subscription lifecycle

```
  Stripe Checkout ──▶ webhook (checkout.session.completed / invoice.paid)
                        └─ set expires_at = period_end
  webhook (subscription.deleted / past_due)
                        └─ leave expires_at; page flips to "expired" once now() > expires_at
```

## Public page states

```
  GET /dashboard/<username>
    username unknown        → 404
    expires_at <= now()     → "Subscription expired / paused" page (data retained)
    else                    → render totals + best-lap table
```

Trade-off accepted: the "expired" page reveals the username exists (vs a 404). Acceptable
for a bragging-rights product; data is retained for instant restore on renewal. Consider a
"purge after N months expired" job later.

## Token lifecycle (security backbone)

- Issued once on account creation; shown once; stored hashed.
- **Regenerate** invalidates the old hash and issues a new token (covers config-file leak).
- Optionally per-instance tokens (multiple gaming PCs) — revoke one without the others.
- Token is a **write-only** credential scoped to that one account's publish; it cannot read
  other accounts or change billing.

## Alternatives considered

- **Supabase-only (no Worker), public page via Supabase Edge Functions / hosting.** Viable;
  Worker preferred only to reuse the existing Cloudflare deploy path for `landing/`.
- **Embed the public page into the existing `landing/` site.** Rejected: landing is a
  static marketing site; the dashboard needs dynamic per-username server rendering + a DB.
- **Local app talks to Supabase directly (no Worker contract).** Rejected: couples the
  self-hosted app to Supabase credentials/SDK; the thin owned `/publish` contract keeps the
  local app storage-agnostic.

## Rollback

Independently deployable/removable. Tearing it down does not affect the local app beyond
publishes failing (recorded as failures locally). No data in this project is a source of
truth — every account can re-publish from its local H2.
