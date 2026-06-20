# Public Dashboard — seed bundle for a SEPARATE project

> **This is not part of the local app.** These files describe a **separate cloud project**
> (the public dashboard) that lives in its own repository. They are staged here only so the
> design thinking is captured alongside the local-app change (`openspec/changes/add-cloud-publish`)
> that pairs with it. When the new repo is created, lift `proposal.md`, `design.md`, and
> `specs/` into that repo's `openspec/` and delete this folder.

## Why two projects

| | This repo (local app) | The cloud project (this folder) |
|---|---|---|
| Deployment | Self-hosted, on the user's LAN | Cloud (Cloudflare Worker + Supabase) |
| Role | Source of truth; **sends** `POST /publish` | **Receives** `/publish`; renders public page |
| Owns | Aggregation, token config, publish triggers | Sign-in, username, token issuance, Stripe, storage, public page |
| Network | One-way outbound only | Public internet ingress |

The two projects share **one contract**: `POST /publish` (defined identically in both
specs). That HTTP call is the only coupling. Keeping them as separate repos keeps the
self-hosted app free of any cloud SDK, credentials, or payment code.

## Contents

- `proposal.md` — why/what/impact for the cloud project.
- `design.md` — architecture, schema, hosting, the shared contract, alternatives, rollback.
- `specs/public-dashboard/spec.md` — requirements (auth, token, publish receiver, payment, public page).
