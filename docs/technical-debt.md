# Technical debt

- DONE — `LapRepository.bestPerTrackQuery` (`allTimeBest=true`) now dedups at the DB level via `ROW_NUMBER() OVER (PARTITION BY track ORDER BY lap_time, id)` rather than pulling all matching rows and grouping in memory. Sort and pagination stay server-side.
