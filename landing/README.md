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

Two options. **Direct upload (no Git connection)** is the default below; the
Git-connected option follows.

### Direct upload with Wrangler (no Git)

One-time setup:

```bash
cd landing
npx wrangler login                                            # browser OAuth
npx wrangler pages project create laptime-insights --production-branch=main
```

Deploy (build + upload):

```bash
cd landing
pnpm run deploy            # production (--branch=main)
pnpm run deploy:preview    # preview URL (--branch=preview)
```

Re-run `pnpm run deploy` anytime to ship — no CI, no repo connection.

**Headless / CI auth** (instead of `wrangler login`) — create an API token in
the Cloudflare dashboard (My Profile → API Tokens; scope **Account › Cloudflare
Pages › Edit**), then:

```bash
CLOUDFLARE_API_TOKEN=... CLOUDFLARE_ACCOUNT_ID=... pnpm run deploy
```

### Git-connected (alternative)

In the Cloudflare dashboard → Pages → Create project → connect this repo, then set:

| Setting                | Value                        |
| ---------------------- | ---------------------------- |
| Production branch      | `main`                       |
| Root directory         | `landing`                    |
| Build command          | `pnpm install && pnpm build` |
| Build output directory | `dist`                       |

Pages then gives a free per-PR preview URL on every pull request.

### Domain / DNS plan

- **Apex `laptimeinsights.com`** → this Pages project (the landing page).
- The app itself is **self-hosted** — users download and run it on their own
  network (same network as their ACC server). There is **no hosted app URL**.
  All CTAs on the landing page point to the GitHub releases/download page
  (`https://github.com/prule/laptime-insights-server/releases`).

DNS records and the custom-domain binding are configured in the Cloudflare
dashboard (not in code). If the download location changes, update the CTA links
in `index.html`.

## Register interest (waiting list)

The `#register-interest` section captures emails into a **Google Sheet** waiting
list. The page is static (no backend): on submit, JS sends a `no-cors` POST
straight to a **Google Form**, and the form's linked Sheet is the list.

The form URL and field id are placeholder constants at the top of the inline
`<script>` in `index.html` — until they're set, the form shows a friendly
"not available yet" message and logs a console warning.

### One-time setup

1. Create a **Google Form** with a single short-answer question for the email
   (mark it required). In the form's settings, link it to a **Google Sheet**
   ("Responses" tab → Link to Sheets) — this Sheet is the waiting list.
2. Get the **`formResponse` URL**: open the live form, copy its URL, and replace
   the trailing `/viewform` with `/formResponse`. It looks like
   `https://docs.google.com/forms/d/e/FORM_ID/formResponse`.
3. Get the **email field id**: in the live form, the email `<input>` has a
   `name` like `entry.1234567890`. Find it via the form's "Get pre-filled link"
   (fill in a value, copy link, read the `entry.<id>` from the URL) or by
   inspecting the input in browser dev tools.
4. In `index.html`, set the two constants:

   ```js
   const FORM_ACTION = "https://docs.google.com/forms/d/e/FORM_ID/formResponse";
   const EMAIL_ENTRY = "entry.1234567890";
   ```

5. Rebuild (`pnpm build`) and deploy.

> **Note:** the `no-cors` POST returns an opaque response, so the page treats a
> completed request as success — confirm submissions actually land by checking
> the linked Sheet. There is no spam protection yet (see `docs/technical-debt.md`).

## Assets — placeholders to replace

The following are **placeholder SVGs** and should be swapped for real exports:

- `assets/dashboard-preview.svg` — replace with a real dashboard screenshot (PNG).
- `assets/og-image.svg` — replace with a real 1200×630 social share image (PNG).
- `assets/favicon.svg` — replace with the real brand mark if/when one exists.

> **Keep screenshots fresh:** when the dashboard UI changes meaningfully,
> re-capture `dashboard-preview` so the landing page doesn't misrepresent the
> product.
