import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import {
  useSession,
  useSessionBestLap,
  useSessionLaps,
  useTrackBestLap,
} from "../api/queries";
import type { LapResource } from "../api/types";
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
  const sessionBestQuery = useSessionBestLap(uid);
  const trackBestQuery = useTrackBestLap(sessionQuery.data?.track ?? null);

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
  const sessionBest = sessionBestQuery.data ?? null;
  const trackBest = trackBestQuery.data ?? null;

  const compareUrl = (lap1: string, lap2: string) =>
    `/compare?track=${encodeURIComponent(session.track ?? "")}&lap1=${lap1}&lap2=${lap2}`;

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
            <div className="grid grid-cols-[60px_120px_120px_100px_90px_200px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
              <div>Lap</div>
              <div>Recorded</div>
              <div>Lap time</div>
              <div>Δ to best</div>
              <div>Status</div>
              <div>Compare</div>
            </div>
            {laps.map((lap) => (
              <LapRow
                key={lap.uid}
                lap={lap}
                bestSoFar={stats.best}
                sessionBest={sessionBest}
                trackBest={trackBest}
                onCompare={(other) => navigate(compareUrl(lap.uid, other))}
              />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}

function LapRow({
  lap,
  bestSoFar,
  sessionBest,
  trackBest,
  onCompare,
}: {
  lap: LapResource;
  bestSoFar: number | null;
  sessionBest: LapResource | null;
  trackBest: LapResource | null;
  onCompare: (otherLapUid: string) => void;
}) {
  // "vs session best" — only meaningful for valid laps that aren't already the
  // session's best. "vs track PB" — same rule, plus the track PB must exist
  // and (rarely) might be this very lap.
  const sessionBestUid = sessionBest?.uid;
  const trackBestUid = trackBest?.uid;
  const canVsSessionBest =
    lap.valid && !!sessionBestUid && sessionBestUid !== lap.uid;
  const canVsTrackBest =
    lap.valid && !!trackBestUid && trackBestUid !== lap.uid;

  return (
    <div className="grid grid-cols-[60px_120px_120px_100px_90px_200px] items-center gap-3 border-b border-border/40 px-3 py-2 last:border-b-0 hover:bg-surface-hover">
      <div className="font-mono text-xs text-text-muted">#{lap.lapNumber}</div>
      <div className="font-mono text-xs text-text-muted">{formatTime(lap.recordedAt)}</div>
      <div className={`font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`}>
        {lap.valid ? formatLapTime(lap.lapTime) : <span className="text-text-dim">INVAL</span>}
      </div>
      <div>
        {lap.valid ? (
          <Delta ms={lap.lapTime} referenceMs={bestSoFar} />
        ) : (
          <span className="text-text-dim">—</span>
        )}
      </div>
      <div className="font-mono text-[11px]">
        {lap.personalBest && <span className="text-ok">PB</span>}
        {!lap.valid && <span className="text-accent">INVALID</span>}
      </div>
      <div className="flex gap-1">
        <CompareButton
          label="vs best"
          title={
            !canVsSessionBest && lap.uid === sessionBestUid
              ? "This lap is the session's best"
              : "Compare against this session's fastest valid lap"
          }
          enabled={canVsSessionBest}
          onClick={() => sessionBestUid && onCompare(sessionBestUid)}
        />
        <CompareButton
          label="vs PB"
          title={
            !canVsTrackBest && lap.uid === trackBestUid
              ? "This lap is the track PB"
              : "Compare against the all-time fastest valid lap at this track"
          }
          enabled={canVsTrackBest}
          onClick={() => trackBestUid && onCompare(trackBestUid)}
        />
      </div>
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
