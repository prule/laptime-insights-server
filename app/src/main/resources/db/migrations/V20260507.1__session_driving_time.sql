-- Drop the unused FINISHED_AT column — sessions are no longer explicitly "finished".
-- Activity is now derived from the cumulative DRIVING_TIME_MS aggregate maintained on lap insert.
alter table SESSION drop column FINISHED_AT;

-- Cumulative time (ms) the player's car spent on track, summed from LAP.LAP_TIME for laps where
-- LAP.CAR_ID = SESSION.PLAYER_CAR_ID. Maintained transactionally by CreateLapService; default 0
-- so existing sessions and any session row created before its first lap stay numeric (not null).
alter table SESSION add column DRIVING_TIME_MS bigint not null default 0;

-- Backfill existing rows from the lap data we already have. Sessions with no PLAYER_CAR_ID set
-- (e.g. seeded before EntryListCar arrived) sum to 0, which matches the column default.
update SESSION s
set DRIVING_TIME_MS = coalesce(
    (select sum(l.LAP_TIME) from LAP l where l.SESSION_ID = s.ID and l.CAR_ID = s.PLAYER_CAR_ID),
    0
);
