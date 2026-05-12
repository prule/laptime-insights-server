# Technical debt

- DONE — `LapRepository.bestPerTrackQuery` (`allTimeBest=true`) now dedups at the DB level via `ROW_NUMBER() OVER (PARTITION BY track ORDER BY lap_time, id)` rather than pulling all matching rows and grouping in memory. Sort and pagination stay server-side.
- DONE — Added `GET /api/1/laps/aggregate?groupBy={track|day|week|month}` server-side `COUNT(*) GROUP BY` endpoint. `OverviewScreen` now drives **Laps per week/month** and **Tracks practiced** via this aggregate; the 1000-row `lapsQuery` is gone.
- `sessionsQuery` (size 100) still drives the driving-time-per-bucket chart by reducing `session.drivingTimeMs` client-side. Add a `/api/1/sessions/aggregate?groupBy={day|week|month}&metric=drivingTimeMs` endpoint so the chart consumes a small server-aggregated response and stays correct past 100 sessions in range.
