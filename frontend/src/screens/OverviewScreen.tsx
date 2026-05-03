import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { SectionHeader } from "../components/ui/SectionHeader";
import { StatCard } from "../components/ui/StatCard";
import { BarChart } from "../components/ui/BarChart";
import { ErrorState, LoadingState } from "../components/ui/States";
import { SessionRow } from "../components/SessionRow";
import { formatLapTime, formatNumber } from "../lib/format";

const ONE_DAY_MS = 86_400_000;

/** Group dates into 8 buckets ending at the most recent session. */
function groupByWeek(timestamps: string[]): { label: string; value: number }[] {
  if (timestamps.length === 0) return [];
  const sortedMs = timestamps.map((t) => new Date(t).getTime()).sort((a, b) => a - b);
  const last = sortedMs[sortedMs.length - 1]!;
  const weekStarts = Array.from({ length: 8 }, (_, i) => last - (7 - i) * 7 * ONE_DAY_MS);
  return weekStarts.map((start, i) => {
    const end = i < weekStarts.length - 1 ? weekStarts[i + 1]! : last + ONE_DAY_MS;
    const count = sortedMs.filter((t) => t >= start && t < end).length;
    const date = new Date(start);
    return {
      label: date.toLocaleDateString("en-GB", { day: "2-digit", month: "short" }),
      value: count,
    };
  });
}

export function OverviewScreen() {
  const navigate = useNavigate();
  const sessionsQuery = useSessions({ size: 100, sort: "startedAt:DESC" });
  const lapsQuery = useLaps({ size: 1000, sort: "lapTime:ASC" });

  const stats = useMemo(() => {
    const sessions = sessionsQuery.data?.items ?? [];
    const laps = lapsQuery.data?.items ?? [];
    const validLaps = laps.filter((l) => l.valid);
    const bestLap = validLaps[0]?.lapTime ?? null;
    const avgMs =
      validLaps.length > 0
        ? validLaps.reduce((acc, l) => acc + l.lapTime, 0) / validLaps.length
        : null;
    return {
      totalSessions: sessionsQuery.data?.total ?? sessions.length,
      totalLaps: lapsQuery.data?.total ?? laps.length,
      bestLap,
      avgLap: avgMs,
    };
  }, [sessionsQuery.data, lapsQuery.data]);

  const sessionStarts = useMemo(
    () => (sessionsQuery.data?.items ?? []).map((s) => s.startedAt).filter((t): t is string => !!t),
    [sessionsQuery.data],
  );

  const lapsPerWeek = useMemo(() => {
    const sessions = sessionsQuery.data?.items ?? [];
    const laps = lapsQuery.data?.items ?? [];
    if (sessions.length === 0) return [];
    // Distribute lap counts across weeks using the lap's recordedAt.
    return groupByWeek(laps.map((l) => l.recordedAt));
  }, [sessionsQuery.data, lapsQuery.data]);

  if (sessionsQuery.isLoading || lapsQuery.isLoading) {
    return <div className="p-8"><LoadingState /></div>;
  }
  if (sessionsQuery.isError) {
    return (
      <div className="p-8">
        <ErrorState error={sessionsQuery.error} onRetry={() => sessionsQuery.refetch()} />
      </div>
    );
  }

  const recentSessions = (sessionsQuery.data?.items ?? []).slice(0, 4);

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <div className="mb-7 flex items-center gap-5 border-b border-border pb-6">
        <div className="flex h-[52px] w-[52px] items-center justify-center rounded-[10px] border border-accent/[0.3] bg-gradient-to-br from-accent/20 to-accent/10 font-mono text-lg font-bold text-accent">
          ◈
        </div>
        <div>
          <div className="font-sans text-xl font-semibold text-text">LapTime Insights</div>
          <div className="font-sans text-[13px] text-text-muted">
            Telemetry across {stats.totalSessions} sessions
          </div>
        </div>
        <div className="ml-auto text-right">
          <div className="font-mono text-[11px] tracking-[0.08em] text-text-muted">PERSONAL BEST</div>
          <div className="font-mono text-2xl font-bold text-ok">{formatLapTime(stats.bestLap)}</div>
        </div>
      </div>

      <div className="mb-6 grid grid-cols-4 gap-3">
        <StatCard label="Total Sessions" value={formatNumber(stats.totalSessions)} accent="cyan" sub="all-time" />
        <StatCard label="Total Laps" value={formatNumber(stats.totalLaps)} accent="accent" sub="all-time" />
        <StatCard label="Best Lap" value={formatLapTime(stats.bestLap)} accent="ok" />
        <StatCard label="Avg Lap" value={formatLapTime(stats.avgLap)} accent="warn" sub="across valid laps" />
      </div>

      <div className="mb-6 grid grid-cols-2 gap-4">
        <Card>
          <SectionHeader title="Laps per week" sub="last 8 weeks" />
          <BarChart data={lapsPerWeek} colorClass="cyan" height={90} />
        </Card>
        <Card>
          <SectionHeader title="Sessions per week" sub="last 8 weeks" />
          <BarChart data={groupByWeek(sessionStarts)} colorClass="accent" height={90} />
        </Card>
      </div>

      <Card>
        <SectionHeader title="Recent sessions" action="View all" onAction={() => navigate("/sessions")} />
        <div className="flex flex-col gap-1">
          {recentSessions.map((s) => (
            <SessionRow key={s.uid} session={s} />
          ))}
        </div>
      </Card>
    </div>
  );
}
