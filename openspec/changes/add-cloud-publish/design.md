# Design — cloud-publish (local app side)

## Context

This is cross-cutting (domain → application → adapters → infrastructure → frontend), adds
a new outbound network dependency, and introduces the first cloud integration in an
otherwise LAN-only product. So a design note is warranted.

## The seam

The boundary between this repo and the public-dashboard project is a single HTTP call.
This repo is the **client**; the cloud project is the **server**. Neither shares code —
they share only the contract below. Keep the payload defined in exactly one place
conceptually (this spec) and mirrored in the cloud project's spec.

```
  ┌──────── this repo: local app ────────┐         ┌──── separate repo: public dashboard ────┐
  │ H2 (source of truth)                  │         │  receives POST /publish                  │
  │   └ aggregate (Session+Lap, valid)    │  HTTPS  │  verify Bearer token → account           │
  │   └ build snapshot ───────────────────┼────────▶│  check subscription active               │
  │   └ POST /publish  (Bearer token)     │         │  REPLACE snapshot for account            │
  │                                       │         │  serve /dashboard/<username>             │
  └───────────────────────────────────────┘         └──────────────────────────────────────────┘
        owns up to the request                              owns from receipt onward
```

## The shared contract (authoritative copy lives in spec.md)

```
POST {cloudBaseUrl}/publish
Authorization: Bearer <token>
Content-Type: application/json

{
  "publishedAt": "2026-06-18T09:30:00Z",
  "totals": {
    "sessions": 142,
    "laps": 3580,
    "distanceKm": 18240.5,
    "drivingTimeSeconds": 345600
  },
  "bests": [
    { "track": "spa", "car": "ferrari-296-gt3", "bestLapMs": 138420 }
  ]
}
```

- **Full-snapshot replace.** Each publish carries the user's complete current summary.
  The cloud replaces all prior rows for the account. This is idempotent and self-healing
  — no incremental reconciliation, no partial state. Manual and periodic triggers send
  the identical request.
- **Valid laps only** feed `bests` (`Lap.valid = true`). Totals count all sessions/laps.
- Success = HTTP 2xx. The local app records the result; it does not parse a body.

## Aggregation source

Both aggregates come from Session + Lap **only** — never `RealtimeCarUpdate` or
telemetry samples (size + privacy). `track`/`car` come from the lap/session columns added
in the `V20260507.*` and `V20260504.*` migrations. No schema change: reads only.

## Triggers: one path, two callers

Manual button and periodic timer both invoke the same `PublishSummaryUseCase`. The timer
is a coroutine scheduler started only when `publishInterval != off` and a token is set.
There is no separate "auto" feature — just two callers of one action.

```
  manual route  ─┐
                 ├─▶ PublishSummaryUseCase.publish() ─▶ buildSnapshot() ─▶ http POST
  periodic timer ─┘
```

## Configuration

| Key                  | Default | Notes                                        |
|----------------------|---------|----------------------------------------------|
| `PUBLISH_CLOUD_URL`  | unset   | Cloud base URL. Unset ⇒ publishing disabled. |
| `PUBLISH_TOKEN`      | unset   | Bearer token from the cloud account.         |
| `PUBLISH_INTERVAL`   | `off`   | `off \| 15m \| 1h \| daily`.                 |

Publishing is **disabled unless both URL and token are set**. A default install is
unaffected and never makes an outbound call.

## HATEOAS / feature gating

Per the project convention, `_links` = capabilities, `enabledFeatures` = UI toggles.
The publish capability link (and `enabledFeatures.cloudPublish`) is present only when
publishing is configured, so the frontend follows the link rather than hardcoding the
route, and hides the button entirely when unconfigured.

## Alternatives considered

- **Sync raw laps/telemetry and aggregate in the cloud.** Rejected for v1: larger payload,
  more privacy surface, and the public page only needs aggregates. Revisit if trends/
  per-lap detail become public later (would change the payload, not the seam).
- **Incremental/delta sync.** Rejected: snapshot replace is simpler and idempotent; the
  data is tiny so re-sending the whole summary is cheap.
- **Supabase client SDK directly from the Kotlin server.** Rejected: couples the local app
  to Supabase internals and credentials. A thin owned HTTP contract keeps the local app
  agnostic to the cloud's storage/auth choices.
- **Cloud pulls from the LAN.** Impossible — the cloud cannot reach into a private network.

## Rollback

Fully additive and opt-in. Rollback = unset `PUBLISH_CLOUD_URL`/`PUBLISH_TOKEN` (runtime
disable) or revert the change (no schema migration to undo, no inbound contract others
depend on within this repo). The cloud project is independently deployable/removable.
