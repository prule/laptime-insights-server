# Technical debt

- DONE — `LapRepository.bestPerTrackQuery` (`allTimeBest=true`) now dedups at the DB level via `ROW_NUMBER() OVER (PARTITION BY track ORDER BY lap_time, id)` rather than pulling all matching rows and grouping in memory. Sort and pagination stay server-side.
- DONE — Added `GET /api/1/laps/aggregate?groupBy={track|day|week|month}` server-side `COUNT(*) GROUP BY` endpoint. `OverviewScreen` now drives **Laps per week/month** and **Tracks practiced** via this aggregate; the 1000-row `lapsQuery` is gone.
- DONE — Added `GET /api/1/sessions/aggregate?groupBy={day|week|month}` that returns per-bucket `count` and `drivingTimeMs`. `OverviewScreen` now drives the **Sessions per …**, **Driving time per …** and **Driving Time** stat card from this single request instead of reducing `sessionsQuery.items` client-side.
- Streak in `OverviewScreen` still iterates session items (size 100). A dedicated lightweight "active days" aggregate (or just exposing the streak count on the server) would let us drop the items dependency entirely.
- `QueryExtension.firstOrNull` resolves sort fields with `sortableFields.mapping[it.field]!!` while `paginate` uses `mapNotNull`. The two paths handle unknown fields inconsistently — `firstOrNull` will NPE where `paginate` silently drops. Align them (probably on the `mapNotNull` behaviour).
- `GET /api/1/sessions` `to` parameter is documented in OpenAPI as **exclusive** but `SessionSearchCriteria.toQuery()` applies it with `lessEq` (inclusive). Pick one and align the docs and the SQL. Same likely applies to `from` — confirm the intent.
- Unknown `sort` field names in `GET /api/1/sessions` are silently dropped rather than returning `400`. Documented behaviour is intentional (forward-compat), but it should be revisited — at minimum log at debug level so misuse is discoverable.

## Landing page (landing/)

- Landing page screenshot (`landing/assets/dashboard-preview.svg`) and social share image (`landing/assets/og-image.svg`) are placeholder SVGs. Replace with real PNG exports of the dashboard, and re-capture the screenshot whenever the dashboard UI changes meaningfully so the landing page stays accurate.
- Cloudflare Pages project still needs to be created/connected in the Cloudflare dashboard (root dir `landing`, build `pnpm install && pnpm build`, output `dist`), plus DNS: apex `laptimeinsights.com` → Pages, app on `app.laptimeinsights.com`. This is a one-time manual infra step — see `landing/README.md`.
- If the landing site grows beyond a single page, migrate from hand-written HTML to Astro or Vite to avoid duplicated markup (out of scope for the initial change).
