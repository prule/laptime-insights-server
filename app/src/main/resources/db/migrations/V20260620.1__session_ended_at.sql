-- When the session finished. Set by the live ingestion layer when it detects a session boundary
-- (a new ACC session identity or a terminal session phase — see SessionTracker). Nullable: in-progress
-- sessions and legacy rows recorded before end detection existed stay null. No backfill — past end
-- times were never captured and cannot be reconstructed.
alter table SESSION add column ENDED_AT timestamp null;
