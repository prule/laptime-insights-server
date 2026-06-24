import { useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  useSession,
  useSessionBestLap,
  useSessionLaps,
  useTrackBestLap,
} from "../api/queries";
import { Badge } from "../components/ui/Badge";
import { Card } from "../components/ui/Card";
import { CarFilterBar } from "../components/ui/CarFilterBar";
import { LapTable } from "../components/LapTable";
import { SectionHeader } from "../components/ui/SectionHeader";
import {
  formatSortParam,
  parseSortParam,
  type SortState,
} from "../components/ui/SortableHeader";
import { Sparkline } from "../components/ui/Sparkline";
import { StatCard } from "../components/ui/StatCard";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { getString, useUrlState } from "../hooks/useUrlState";
import { formatDate, formatDrivingTime, formatLapTime, formatTime } from "../lib/format";
import { useFeatureEnabled } from "../providers/FeaturesProvider";

export function SessionDetailScreen() {
  const { uid } = useParams();
  const navigate = useNavigate();
  const [params, , setMany] = useUrlState();
  const sessionQuery = useSession(uid);
  const session = sessionQuery.data;

  // Sort lives in the URL (`sort=field:ORDER`) so the ordering deep-links and survives reload,
  // matching the Laps and Sessions tables. Default `lapNumber:ASC` preserves today's order. The
  // backend applies the sort — we just follow the session's HATEOAS `laps` link with the param.
  const sort: SortState = parseSortParam(getString(params, "sort")) ?? {
    field: "lapNumber",
    order: "ASC",
  };
  const setSort = (next: SortState | null) => {
    setMany({ sort: formatSortParam(next) });
  };

  const lapsQuery = useSessionLaps(session, { sort: formatSortParam(sort) });
  const sessionBestQuery = useSessionBestLap(session);
  const trackBestQuery = useTrackBestLap(session?.track ?? null);
  const sessionsEnabled = useFeatureEnabled("sessions");
  const compareEnabled = useFeatureEnabled("compare");

  // null = all cars shown
  const [selectedCarId, setSelectedCarId] = useState<number | null>(null);

  // Player's carId comes from the session resource (set by EntryListCar in ClientInitializer).
  const playerCarId: number | null = sessionQuery.data?.playerCarId ?? null;

  // Distinct car numbers present in this session (player first).
  const carIds = useMemo(() => {
    const laps = lapsQuery.data?.items ?? [];
    const all = Array.from(new Set(laps.map((l) => l.carId)));
    all.sort((a, b) => (a === playerCarId ? -1 : b === playerCarId ? 1 : a - b));
    return all;
  }, [lapsQuery.data, playerCarId]);

  // Laps visible after car filter.
  const visibleLaps = useMemo(() => {
    const laps = lapsQuery.data?.items ?? [];
    return selectedCarId !== null ? laps.filter((l) => l.carId === selectedCarId) : laps;
  }, [lapsQuery.data, selectedCarId]);

  // The trend Sparkline is a timeline, not a ranking — keep it in lap-number order regardless of
  // the table's current sort. Sort a copy so the table's backend-sorted order is untouched.
  const trendValues = useMemo(
    () =>
      visibleLaps
        .filter((l) => l.valid)
        .slice()
        .sort((a, b) => a.lapNumber - b.lapNumber)
        .map((l) => l.lapTime),
    [visibleLaps],
  );

  // Stats reflect the currently filtered car (or player car when showing all).
  const stats = useMemo(() => {
    const scopedLaps =
      selectedCarId !== null
        ? visibleLaps
        : visibleLaps.filter((l) => playerCarId === null || l.carId === playerCarId);
    const valid = scopedLaps.filter((l) => l.valid);
    const best = valid.reduce<number | null>(
      (acc, l) => (acc === null || l.lapTime < acc ? l.lapTime : acc),
      null,
    );
    const avg = valid.length > 0 ? valid.reduce((s, l) => s + l.lapTime, 0) / valid.length : null;
    return { lapCount: scopedLaps.length, validCount: valid.length, best, avg };
  }, [visibleLaps, selectedCarId, playerCarId]);

  if (sessionQuery.isLoading) return <div className="p-8"><LoadingState /></div>;
  if (sessionQuery.isError)
    return (
      <div className="p-8">
        <ErrorState error={sessionQuery.error} onRetry={() => sessionQuery.refetch()} />
      </div>
    );
  if (!session) return <div className="p-8"><EmptyState title="Session not found" /></div>;

  const allLaps = lapsQuery.data?.items ?? [];
  const sessionBest = sessionBestQuery.data ?? null;
  const trackBest = trackBestQuery.data ?? null;

  const compareUrl = (anchor: string, challenger: string) =>
    `/compare?track=${encodeURIComponent(session.track ?? "")}&anchor=${anchor}&challenger=${challenger}`;

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      {sessionsEnabled && (
        <button
          onClick={() => navigate("/sessions")}
          className="mb-4 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
        >
          ← Back to sessions
        </button>
      )}

      <Card className="mb-4">
        <div className="flex items-center gap-5">
          <div className="flex flex-col gap-1">
            <div className="flex items-center gap-2">
              <Badge type={session.sessionType} />
              <span className="font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted">
                {session.simulator}
              </span>
            </div>
            <div className="font-sans text-2xl font-semibold text-text">{session.track ?? "Unknown"}</div>
            <div className="font-sans text-sm text-text-muted">{session.car ?? "Unknown car"}</div>
            <div className="font-mono text-xs text-text-muted">
              {formatDate(session.startedAt)} · {formatTime(session.startedAt)} ·{" "}
              {formatDrivingTime(session.drivingTimeMs)}
            </div>
          </div>
        </div>
      </Card>

      <div className="mb-4 grid grid-cols-4 gap-3">
        <StatCard label="Laps" value={stats.lapCount} accent="cyan" small />
        <StatCard label="Valid" value={stats.validCount} accent="ok" small />
        <StatCard label="Best Lap" value={formatLapTime(stats.best)} accent="warn" small />
        <StatCard label="Avg Lap" value={formatLapTime(stats.avg)} accent="muted" small />
      </div>

      <Card>
        <SectionHeader
          title="Laps"
          action="Trend"
          sub={
            allLaps.length > 0 ? (
              <span className="text-text-dim">
                <Sparkline values={trendValues} color="#00d4ff" />
              </span>
            ) : undefined
          }
        />

        {allLaps.length > 0 && (
          <div className="mb-3">
            <CarFilterBar
              cars={carIds.map((id) => ({
                value: String(id),
                label: `Car ${id}${id === playerCarId ? " (you)" : ""}`,
                isPlayer: id === playerCarId,
              }))}
              selected={selectedCarId !== null ? String(selectedCarId) : null}
              onChange={(v) => setSelectedCarId(v !== null ? Number(v) : null)}
            />
          </div>
        )}

        {lapsQuery.isLoading && <LoadingState />}
        {lapsQuery.isError && (
          <ErrorState error={lapsQuery.error} onRetry={() => lapsQuery.refetch()} />
        )}
        {lapsQuery.data && allLaps.length === 0 && <EmptyState title="No laps recorded" />}
        {visibleLaps.length === 0 && allLaps.length > 0 && (
          <EmptyState title="No laps for this car" />
        )}
        {visibleLaps.length > 0 && (
          <LapTable
            laps={visibleLaps}
            sortableFields={lapsQuery.data?.sortable}
            sort={sort}
            onSortChange={setSort}
            onSessionClick={(uid) => navigate(`/sessions/${uid}`)}
            isRowDimmed={(lap) => playerCarId !== null && lap.carId !== playerCarId}
            extraColumns={
              compareEnabled
                ? [
                    {
                      header: "Compare",
                      width: "220px",
                      cell: (lap) => {
                        const sessionBestUid = sessionBest?.uid;
                        const trackBestUid = trackBest?.uid;
                        const canVsSessionBest =
                          lap.valid && !!sessionBestUid && sessionBestUid !== lap.uid;
                        const canVsTrackBest =
                          lap.valid && !!trackBestUid && trackBestUid !== lap.uid;
                        return (
                          <div className="flex flex-wrap gap-1">
                            <CompareButton
                              label="vs best"
                              title={
                                !canVsSessionBest && lap.uid === sessionBestUid
                                  ? "This lap is the session's best"
                                  : "Compare against this session's fastest valid lap"
                              }
                              enabled={canVsSessionBest}
                              onClick={() =>
                                sessionBestUid && navigate(compareUrl(lap.uid, sessionBestUid))
                              }
                            />
                            <CompareButton
                              label="vs PB"
                              title={
                                !canVsTrackBest && lap.uid === trackBestUid
                                  ? "This lap is the track PB"
                                  : "Compare against the all-time fastest valid lap at this track"
                              }
                              enabled={canVsTrackBest}
                              onClick={() =>
                                trackBestUid && navigate(compareUrl(lap.uid, trackBestUid))
                              }
                            />
                            <CompareButton
                              label="pick…"
                              title="Open compare screen with this lap pre-selected — pick any other lap to compare against"
                              enabled={true}
                              onClick={() =>
                                navigate(
                                  `/compare?track=${encodeURIComponent(session.track ?? "")}&anchor=${lap.uid}`,
                                )
                              }
                            />
                          </div>
                        );
                      },
                    },
                  ]
                : undefined
            }
          />
        )}
      </Card>
    </div>
  );
}

function CompareButton({
  label,
  title,
  enabled,
  onClick,
}: {
  label: string;
  title: string;
  enabled: boolean;
  onClick: () => void;
}) {
  return (
    <button
      onClick={onClick}
      disabled={!enabled}
      title={title}
      className="rounded border border-border px-2 py-1 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted transition-colors hover:border-cyan/40 hover:text-cyan disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:border-border disabled:hover:text-text-muted"
    >
      {label}
    </button>
  );
}
