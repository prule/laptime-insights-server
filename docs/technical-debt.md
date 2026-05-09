# Technical debt

- `LapRepository.bestPerTrack` (`allTimeBest=true`) does in-memory dedup after pulling all matching rows from the DB. Fine for the single-player dashboard scale (dozens of tracks, low thousands of laps), but unbounded — if the dataset grows or the criteria are loose, this materialises every match. If it becomes hot, replace with a window-function query (`ROW_NUMBER() OVER (PARTITION BY track ORDER BY lap_time)` then `WHERE rn=1`) so the DB does the dedup and pagination stays server-side.
