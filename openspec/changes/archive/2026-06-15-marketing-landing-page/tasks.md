## 1. Scaffold isolated landing project

- [x] 1.1 Create `landing/` dir with own `package.json` (pnpm) — Tailwind v4 CLI dep only, no app deps
- [x] 1.2 Add `landing/.gitignore` (node_modules, dist) and Tailwind config/entry `landing/src/styles.css`
- [x] 1.3 Add `build` script (Tailwind CLI compile + copy `index.html`/static files into `landing/dist`) and a `dev`/preview script
- [x] 1.4 Verify `pnpm install && pnpm build` in `landing/` emits `landing/dist` and touches nothing in `app/`/`frontend/`

## 2. Build the page

- [x] 2.1 Create `landing/index.html` with hero (headline, subtext, primary CTA to the app)
- [x] 2.2 Add feature sections (Overview, Sessions, Laps, Compare, streaks, time-range analytics) — copy sourced from `docs/user-guide.md`
- [x] 2.3 Capture dashboard screenshot(s) and add to `landing/assets/`; embed at least one (placeholder SVG embedded; real PNG capture logged in docs/technical-debt.md)
- [x] 2.4 Apply dark theme + responsive Tailwind layout; verify 375px and 1280px render with no horizontal overflow

## 3. SEO and social

- [x] 3.1 Add `<title>`, meta description, canonical URL, Open Graph + Twitter card tags (absolute OG image URL)
- [x] 3.2 Add favicon and OG share image to `landing/assets/`
- [x] 3.3 Add `landing/robots.txt` and `landing/sitemap.xml`; ensure both copied into `dist`

## 4. Cloudflare Pages deployment

- [x] 4.1 Document Pages config in `landing/README.md`: root dir `landing`, build `pnpm install && pnpm build`, output `landing/dist`, local preview steps
- [x] 4.2 Document DNS/domain plan: apex `laptimeinsights.com` → Pages, app on `app.` subdomain; CTA points there
- [ ] 4.3 Connect Cloudflare Pages project and confirm a production + preview deploy succeed — BLOCKED: manual infra step in the Cloudflare account (config documented in landing/README.md + docs/technical-debt.md)

## 5. Docs and cleanup

- [x] 5.1 Add `landing/README.md` (local dev, build, deploy, note to refresh screenshots when dashboard UI changes)
- [x] 5.2 Reference the landing site in root `README.md`
- [x] 5.3 Log any follow-ups (e.g. migrate to Astro if multi-page, screenshot-drift) in `docs/technical-debt.md`
