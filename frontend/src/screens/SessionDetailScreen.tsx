import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSession, useSessionLaps } from "../api/queries";
import { Badge } from "../components/ui/Badge";
import { Card } from "../components/ui/Card";
import { Delta } from "../components/ui/Delta";
import { SectionHeader } from "../components/ui/SectionHeader";
import { Sparkline } from "../components/ui/Sparkline";
import { StatCard } from "../components/ui/StatCard";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { formatDate, formatDuration, formatLapTime, formatTime } from "../lib/format";

export function SessionDetailScreen() {
  const { uid } = useParams();
  const navigate = useNavigate();
  const sessionQuery = useSession(uid);
  const lapsQuery = useSessionLaps(uid);

  const stats = useMemo(() => {
    const laps = lapsQuery.data?.items ?? [];
    const valid = laps.filter((l) => l.valid);
    const best = valid.reduce<number | null>(
      (acc, l) => (acc === null || l.lapTime < acc ? l.lapTime : acc),
      null,
    );
    const avg = valid.length > 0 ? valid.reduce((s, l) => s + l.lapTime, 0) / valid.length : null;
    return { lapCount: laps.length, validCount: valid.length, best, avg };
  }, [lapsQuery.data]);

  if (sessionQuery.isLoading) return <div className="p-8"><LoadingState /></div>;
  if (sessionQuery.isError)
    return (
      <div className="p-8">
        <ErrorState error={sessionQuery.error} onRetry={() => sessionQuery.refetch()} />
      </div>
    );
  if (!sessionQuery.data) return <div className="p-8"><EmptyState title="Session not found" /></div>;

  const session = sessionQuery.data;
  const laps = lapsQuery.data?.items ?? [];

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <button
        onClick={() => navigate("/sessions")}
        className="mb-4 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
      >
        ← Back to sessions
      </button>

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
              {formatDuration(session.startedAt, session.endedAt)}
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
            laps.length > 0 ? (
              <span className="text-text-dim">
                <Sparkline values={laps.filter((l) => l.valid).map((l) => l.lapTime)} color="#00d4ff" />
              </span>
            ) : undefined
          }
        />
        {lapsQuery.isLoading && <LoadingState />}
        {lapsQuery.isError && (
          <ErrorState error={lapsQuery.error} onRetry={() => lapsQuery.refetch()} />
        )}
        {lapsQuery.data && laps.length === 0 && <EmptyState title="No laps recorded" />}
        {laps.length > 0 && (
          <div className="overflow-hidden rounded border border-border">
            <div className="grid grid-cols-[60px_1fr_120px_100px_100px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
              <div>Lap</div>
              <div>Recorded</div>
              <div>Lap time</div>
              <div>Δ to best</div>
              <div>Status</div>
            </div>
            {laps.map((lap) => (
              <div
                key={lap.uid}
                className="grid grid-cols-[60px_1fr_120px_100px_100px] items-center gap-3 border-b border-border/40 px-3 py-2 last:border-b-0 hover:bg-surface-hover"
              >
                <div className="font-mono text-xs text-text-muted">#{lap.lapNumber}</div>
                <div className="font-mono text-xs text-text-muted">{formatTime(lap.recordedAt)}</div>
                <div className={`font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`}>
                  {lap.valid ? formatLapTime(lap.lapTime) : <span className="text-text-dim">INVAL</span>}
                </div>
                <div>
                  {lap.valid ? <Delta ms={lap.lapTime} referenceMs={stats.best} /> : <span className="text-text-dim">—</span>}
                </div>
                <div className="font-mono text-[11px]">
                  {lap.personalBest && <span className="text-ok">PB</span>}
                  {!lap.valid && <span className="text-accent">INVALID</span>}
                </div>
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
