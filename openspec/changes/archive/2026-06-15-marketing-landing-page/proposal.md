## Why

Laptime Insights has a functional dashboard (`app/` + `frontend/`) but no public-facing
page that explains what it is, who it's for, or why an ACC driver should use it. The SPA
loads straight into the product UI — bad for first impressions, SEO, and sharing a link.
A dedicated marketing landing page lets us promote `laptimeinsights.com` to a cold audience
without coupling marketing content to the app's release cycle or build.

## What Changes

- Add a **completely separate** static landing site under a new top-level `landing/` directory.
- Built as plain HTML + Tailwind CSS (no React, no app dependencies). Independent of `frontend/`.
- Hero, feature highlights (Overview/Sessions/Laps/Compare, streaks, time-range analytics),
  screenshots, and a clear call-to-action linking through to the app.
- SEO + social essentials: meta tags, Open Graph/Twitter cards, favicon, sitemap, robots.txt.
- Deployable to **Cloudflare Pages** via its own build config (separate from the app's CI jobs).
- Own README documenting local dev and deploy. No changes to `app/` or `frontend/`.

## Capabilities

### New Capabilities
- `marketing-landing-page`: Public static landing page promoting laptimeinsights.com — content
  sections, responsive layout, SEO/social metadata, and Cloudflare Pages deployment.

### Modified Capabilities
<!-- None. The landing site is fully isolated; no existing spec requirements change. -->

## Impact

- **New code**: `landing/` directory (HTML, Tailwind config/build, static assets, README).
- **Dependencies**: Tailwind CSS toolchain scoped to `landing/` only (own package.json/lockfile);
  does not touch `frontend/` deps.
- **CI/Deploy**: New Cloudflare Pages project (build command + output dir). Optional GitHub
  Actions/Pages preview wiring kept independent of existing backend/frontend jobs.
- **Domain**: `laptimeinsights.com` apex/root points at the landing site; app served on a
  subpath or subdomain (decided in design.md).
- No impact to backend, frontend SPA, or HATEOAS API.
