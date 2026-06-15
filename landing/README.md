# Laptime Insights — Landing Page

Standalone marketing site for **laptimeinsights.com**. Plain HTML + Tailwind CSS,
built to static files and deployed to **Cloudflare Pages**. Fully isolated from
`app/` and `frontend/` — it has its own `package.json`, lockfile, and build.

## Stack

- Plain `index.html` (no framework, no SPA)
- Tailwind CSS v4 via `@tailwindcss/cli`
- pnpm (matches repo convention)

## Local development

```bash
cd landing
pnpm install
pnpm dev        # Tailwind --watch: rebuilds dist/styles.css on change
```

Open `dist/index.html`, or serve the folder for correct absolute paths:

```bash
pnpm preview    # builds, then serves dist/ on a local port
```

## Build

```bash
pnpm build      # clean → compile minified CSS → copy static files into dist/
```

Output: **`landing/dist`** containing `index.html`, `styles.css`, `robots.txt`,
`sitemap.xml`, and `assets/`.

## Deploy — Cloudflare Pages

Git-connected project. In the Cloudflare dashboard → Pages → Create project →
connect this repo, then set:

| Setting               | Value                       |
| --------------------- | --------------------------- |
| Production branch     | `main`                      |
| Root directory        | `landing`                   |
| Build command         | `pnpm install && pnpm build`|
| Build output directory| `dist`                      |

Pages gives a free per-PR preview URL on every pull request.

### Domain / DNS plan

- **Apex `laptimeinsights.com`** → this Pages project (the landing page).
- The app itself is **self-hosted** — users download and run it on their own
  network (same network as their ACC server). There is **no hosted app URL**.
  All CTAs on the landing page point to the GitHub releases/download page
  (`https://github.com/prule/laptime-insights-server/releases`).

DNS records and the custom-domain binding are configured in the Cloudflare
dashboard (not in code). If the download location changes, update the CTA links
in `index.html`.

## Assets — placeholders to replace

The following are **placeholder SVGs** and should be swapped for real exports:

- `assets/dashboard-preview.svg` — replace with a real dashboard screenshot (PNG).
- `assets/og-image.svg` — replace with a real 1200×630 social share image (PNG).
- `assets/favicon.svg` — replace with the real brand mark if/when one exists.

> **Keep screenshots fresh:** when the dashboard UI changes meaningfully,
> re-capture `dashboard-preview` so the landing page doesn't misrepresent the
> product.
