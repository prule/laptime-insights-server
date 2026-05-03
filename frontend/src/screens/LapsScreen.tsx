import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessionOptions, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import type { SessionResource } from "../api/types";
import { formatLapTime, formatTime } from "../lib/format";

const PAGE_SIZE = 50;

interface SessionFacets {
  car?: string;
  track?: string;
  simulator?: string;
}

/**
 * Lap-search screen.
 *
 * The backend's lap endpoint filters by `sessionUid` only — it has no concept
 * of `car`/`track`/`simulator`. We bridge that gap on the client:
 *
 *   1. fetch sessions matching the facet filters,
 *   2. fetch laps with paging + lap-level filters (valid / PB),
 *   3. only show laps whose `sessionUid` is in the filtered session set,
 *   4. enrich each row with the session's car/track for display.
 *
 * The `useSessions` query is cached separately from `useLaps`, so the join is
 * essentially free after the first fetch.
 */
export function LapsScreen() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [validOnly, setValidOnly] = useState(true);
  const [pbOnly, setPbOnly] = useState(false);
  const [facets, setFacets] = useState<SessionFacets>({});

  const optionsQuery = useSessionOptions();
  // Pull every session matching the facets so we can both populate the join
  // map and know whether a lap belongs to a filtered session.
  const sessionsQuery = useSessions({ ...facets, size: 500, sort: "startedAt:DESC" });

  const lapsQuery = useLaps({
    page,
    size: PAGE_SIZE,
    sort: "lapTime:ASC",
    validLap: validOnly ? true : undefined,
    personalBest: pbOnly ? true : undefined,
  });

  const sessionsByUid = useMemo(() => {
    const map = new Map<string, SessionResource>();
    for (const s of sessionsQuery.data?.items ?? []) map.set(s.uid, s);
    return map;
  }, [sessionsQuery.data]);

  const facetsActive = !!(facets.car || facets.track || facets.simulator);

  const visibleLaps = useMemo(() => {
    const all = lapsQuery.data?.items ?? [];
    if (!facetsActive) return all;
    return all.filter((lap) => sessionsByUid.has(lap.sessionUid));
  }, [lapsQuery.data, sessionsByUid, facetsActive]);

  const setFacet = (key: keyof SessionFacets, value: string | undefined) => {
    setFacets((prev) => ({ ...prev, [key]: value }));
    setPage(1);
  };

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader
          title="Filter"
          sub={
            facetsActive
              ? "Car/track/simulator filter sessions client-side; lap-level toggles hit the API"
              : "Lap-level filters hit /api/1/laps directly"
          }
        />
        <div className="flex flex-wrap items-end gap-3">
          <FilterSelect
            label="Track"
            value={facets.track}
            options={optionsQuery.data?.tracks ?? []}
            onChange={(v) => setFacet("track", v)}
          />
          <FilterSelect
            label="Car"
            value={facets.car}
            options={optionsQuery.data?.cars ?? []}
            onChange={(v) => setFacet("car", v)}
          />
          <FilterSelect
            label="Simulator"
            value={facets.simulator}
            options={optionsQuery.data?.simulators ?? []}
            onChange={(v) => setFacet("simulator", v)}
          />
          <Toggle label="Valid only" value={validOnly} onChange={setValidOnly} />
          <Toggle label="Personal bests" value={pbOnly} onChange={setPbOnly} />
          {(facetsActive || pbOnly || !validOnly) && (
            <button
              onClick={() => {
                setFacets({});
                setValidOnly(true);
                setPbOnly(false);
                setPage(1);
              }}
              className="self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
            >
              Reset
            </button>
          )}
        </div>
      </Card>

      <Card>
        <SectionHeader
          title="Fastest laps"
          sub={
            lapsQuery.data
              ? facetsActive
                ? `${visibleLaps.length} on this page · ${lapsQuery.data.total} total before facet filter`
                : `${lapsQuery.data.total} total · page ${page}`
              : undefined
          }
        />
        {(lapsQuery.isLoading || sessionsQuery.isLoading) && <LoadingState />}
        {lapsQuery.isError && (
          <ErrorState error={lapsQuery.error} onRetry={() => lapsQuery.refetch()} />
        )}
        {lapsQuery.data && visibleLaps.length === 0 && (
          <EmptyState
            title="No laps match"
            description={facetsActive ? "Try clearing the car/track/simulator filters." : "Loosen the toggles or seed the database."}
          />
        )}
        {visibleLaps.length > 0 && (
          <>
            <div className="overflow-hidden rounded border border-border">
              <div className="grid grid-cols-[50px_120px_1fr_1fr_90px_110px_70px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
                <div>#</div>
                <div>Lap time</div>
                <div>Track</div>
                <div>Car</div>
                <div>Sim</div>
                <div>Recorded</div>
                <div>Status</div>
              </div>
              {visibleLaps.map((lap, i) => {
                const session = sessionsByUid.get(lap.sessionUid);
                return (
                  <button
                    key={lap.uid}
                    onClick={() => navigate(`/sessions/${lap.sessionUid}`)}
                    className="grid w-full grid-cols-[50px_120px_1fr_1fr_90px_110px_70px] items-center gap-3 border-b border-border/40 px-3 py-2 text-left last:border-b-0 hover:bg-surface-hover"
                  >
                    <div className="font-mono text-xs text-text-muted">
                      {(page - 1) * PAGE_SIZE + i + 1}
                    </div>
                    <div className={`font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`}>
                      {formatLapTime(lap.lapTime)}
                    </div>
                    <div className="truncate font-sans text-[13px] text-text">
                      {session?.track ?? <span className="text-text-dim">unknown</span>}
                    </div>
                    <div className="truncate font-sans text-[12px] text-text-muted">
                      {session?.car ?? <span className="text-text-dim">unknown</span>}
                    </div>
                    <div className="font-mono text-[11px] uppercase tracking-[0.05em] text-text-muted">
                      {session?.simulator ?? "—"}
                    </div>
                    <div className="font-mono text-xs text-text-muted">{formatTime(lap.recordedAt)}</div>
                    <div className="font-mono text-[11px]">
                      {lap.personalBest && <span className="text-ok">PB</span>}
                      {!lap.valid && <span className="text-accent">INVAL</span>}
                    </div>
                  </button>
                );
              })}
            </div>
            <div className="mt-3 flex items-center gap-2">
              <button
                disabled={page === 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
              >
                Prev
              </button>
              <button
                disabled={!lapsQuery.data || page * PAGE_SIZE >= lapsQuery.data.total}
                onClick={() => setPage((p) => p + 1)}
                className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
              >
                Next
              </button>
            </div>
          </>
        )}
      </Card>
    </div>
  );
}

function Toggle({
  label,
  value,
  onChange,
}: {
  label: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <button
      onClick={() => onChange(!value)}
      className={[
        "self-end rounded border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors",
        value
          ? "border-cyan/30 bg-cyan/10 text-cyan"
          : "border-border text-text-muted hover:bg-surface-hover",
      ].join(" ")}
    >
      {value ? "✓" : "○"} {label}
    </button>
  );
}
