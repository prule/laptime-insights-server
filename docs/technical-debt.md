# Technical debt

- DONE — `LapRepository.bestPerTrackQuery` (`allTimeBest=true`) now dedups at the DB level via `ROW_NUMBER() OVER (PARTITION BY track ORDER BY lap_time, id)` rather than pulling all matching rows and grouping in memory. Sort and pagination stay server-side.
- DONE — Added `GET /api/1/laps/aggregate?groupBy={track|day|week|month}` server-side `COUNT(*) GROUP BY` endpoint. `OverviewScreen` now drives **Laps per week/month** and **Tracks practiced** via this aggregate; the 1000-row `lapsQuery` is gone.
- DONE — Added `GET /api/1/sessions/aggregate?groupBy={day|week|month}` that returns per-bucket `count` and `drivingTimeMs`. `OverviewScreen` now drives the **Sessions per …**, **Driving time per …** and **Driving Time** stat card from this single request instead of reducing `sessionsQuery.items` client-side.
- Streak in `OverviewScreen` still iterates session items (size 100). A dedicated lightweight "active days" aggregate (or just exposing the streak count on the server) would let us drop the items dependency entirely.
