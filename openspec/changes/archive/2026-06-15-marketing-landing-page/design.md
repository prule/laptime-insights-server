## Context

Laptime Insights is a monorepo: Kotlin/Ktor API (`app/`) and React SPA (`frontend/`). There is
no marketing surface — visitors hit the app directly. We want a standalone, fast-loading promo
page for `laptimeinsights.com` that ships and deploys independently of the app. User decisions:
plain HTML + Tailwind, lives in a new `landing/` dir in this repo, deployed to Cloudflare Pages.

## Goals / Non-Goals

**Goals:**
- A single static, responsive landing page isolated from `app/` and `frontend/`.
- Fast (mostly zero-JS), SEO-friendly, shareable (OG/Twitter cards).
- One-command local preview and a documented Cloudflare Pages deploy.
- No coupling to existing build, CI, or runtime.

**Non-Goals:**
- No CMS, blog engine, or multi-page site (single page now; structure allows growth later).
- No React, no SPA, no backend calls, no analytics backend (a script tag may be added later).
- No changes to the app, API, or HATEOAS behavior.
- No separate repository (kept in-repo per decision).

## Decisions

- **Directory**: new top-level `landing/`. Self-contained: `index.html`, `src/styles.css`
  (Tailwind entry), static `assets/` (screenshots, favicon, OG image), `robots.txt`, `sitemap.xml`.
- **Build**: Tailwind CSS via its own `package.json` + lockfile in `landing/` (pnpm, matching repo
  convention). Build = Tailwind CLI compiling `src/styles.css` → `dist/`, with `index.html` and
  static files copied to `dist/`. Output dir: `landing/dist`. Keep tooling minimal — no Vite needed
  for a single static page; a small `build` script (Tailwind CLI + copy) suffices. Reconsider Vite
  only if asset hashing/bundling becomes necessary.
- **Styling**: Tailwind v4 (matches repo), utility classes inline in HTML. Optional small custom
  CSS for hero gradient. Dark theme to match the dashboard's look.
- **Deploy**: Cloudflare Pages, Git-connected. Build command `pnpm install && pnpm build`, output
  directory `landing/dist`, root/base directory `landing`. Pages gives per-PR preview URLs for free.
- **Domain routing**: apex `laptimeinsights.com` → landing site (Pages). App served at a subdomain
  (e.g. `app.laptimeinsights.com`) or subpath — CTA links there. Exact DNS/routing set in Cloudflare
  dashboard at deploy; documented in `landing/README.md`. Default recommendation: `app.` subdomain.
- **SEO/social**: hardcoded meta tags in `index.html`; `sitemap.xml` + `robots.txt` as static files;
  OG image as a static asset referenced by absolute URL.
- **Content source**: feature copy derived from `docs/user-guide.md` (Overview, Sessions, Laps,
  Compare, streaks, time-range selector). Screenshots captured from the running app.

## Risks / Trade-offs

- **Toolchain duplication**: a second Tailwind/pnpm setup in `landing/`. Trade-off accepted for
  isolation; cost is low (one small lockfile). Rollback: delete `landing/` — nothing else depends on it.
- **Plain-HTML maintainability**: as content grows, hand-written HTML gets repetitive. Acceptable for
  a single page; migrate to Astro/Vite later if it becomes multi-page (Non-Goal today).
- **Screenshots drift**: product UI changes can stale the screenshots. Mitigate by noting in
  `landing/README.md` to refresh assets when the dashboard changes; log staleness in technical-debt.
- **Domain config is manual**: DNS + Pages binding done in the Cloudflare dashboard, not in code.
  Documented in README to keep it reproducible.
- **Rollback path**: the landing site is additive and isolated; removing it or pointing DNS back has
  zero effect on `app/` or `frontend/`.
