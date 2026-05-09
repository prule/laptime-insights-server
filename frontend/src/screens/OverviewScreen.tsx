import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessionOptions, useSessions } from "../api/queries";
import { type BucketPlan, useTimeRange } from "../providers/TimeRangeProvider";
import { Card } from "../components/ui/Card";
import { SectionHeader } from "../components/ui/SectionHeader";
import { StatCard } from "../components/ui/StatCard";
import { BarChart } from "../components/ui/BarChart";
import { ErrorState, LoadingState } from "../components/ui/States";
import { SessionRow } from "../components/SessionRow";
import { TrackPracticeChart } from "../components/ui/TrackPracticeChart";
import { AllTimeBestTable } from "../components/AllTimeBestTable";
import { formatLapTime, formatNumber } from "../lib/format";

const ONE_DAY_MS = 86_400_000;
const ONE_WEEK_MS = 7 * ONE_DAY_MS;

/**
 * Group timestamps into N contiguous buckets ending at `anchor`. Bucket width
 * is fixed for `week` but variable for `month` (calendar months step naturally,
 * so each bucket aligns to the start of a real month).
 *
 * `anchor` defaults to the most recent timestamp so an "all" range hugs the
 * data's actual end instead of rendering empty trailing buckets.
 */
function groupByPlan(
  timestamps: string[],
  plan: BucketPlan,
  anchor?: number,
): { label: string; value: number }[] {
  if (timestamps.length === 0) return [];
  const sortedMs = timestamps.map((t) => new Date(t).getTime()).sort((a, b) => a - b);
  const end = anchor ?? sortedMs[sortedMs.length - 1]!;

  const starts: number[] = [];
  if (plan.unit === "week") {
    for (let i = plan.count - 1; i >= 0; i--) {
      starts.push(end - i * ONE_WEEK_MS);
    }
  } else {
    // Walk back month-by-month from the anchor's month start.
    const anchorDate = new Date(end);
    const baseYear = anchorDate.getFullYear();
    const baseMonth = anchorDate.getMonth();
    for (let i = plan.count - 1; i >= 0; i--) {
      starts.push(new Date(baseYear, baseMonth - i, 1).getTime());
    }
  }

  return starts.map((start, i) => {
    const next = i < starts.length - 1 ? starts[i + 1]! : end + ONE_DAY_MS;
    const count = sortedMs.filter((t) => t >= start && t < next).length;
    const date = new Date(start);
    const label =
      plan.unit === "week"
        ? date.toLocaleDateString("en-GB", { day: "2-digit", month: "short" })
        : date.toLocaleDateString("en-GB", { month: "short", year: "2-digit" });
    return { label, value: count };
  });
}

export function OverviewScreen() {
  const navigate = useNavigate();
  const { fromIso, bucketPlan, range } = useTimeRange();
  const from = fromIso ?? undefined;
  const sessionsQuery = useSessions({ size: 100, sort: "startedAt:DESC", from });
  const lapsQuery = useLaps({ size: 1000, sort: "lapTime:ASC", from });
  // All-time best lap per track for the player. Independent of the time-range
  // filter — "all-time" means all-time, regardless of the dashboard window.
  const bestPerTrackQuery = useLaps({
    playerLap: true,
    validLap: true,
    allTimeBest: true,
    size: 100,
    sort: "track:ASC",
  });
  // Full universe of tracks the user has *ever* visited — feeds the practice
  // chart so tracks with zero laps in range still render as "haven't been here
  // lately" placeholders. Not bound to the active range on purpose.
  const optionsQuery = useSessionOptions();

  // Build sessionUid → playerCarId map from the fetched sessions.
  const playerCarIdBySession = useMemo(() => {
    const map = new Map<string, number | null>();
    for (const s of sessionsQuery.data?.items ?? []) map.set(s.uid, s.playerCarId);
    return map;
  }, [sessionsQuery.data]);

  // Filter laps to only those driven by the player in each session.
  const playerLaps = useMemo(() => {
    const laps = lapsQuery.data?.items ?? [];
    return laps.filter((l) => {
      const pcid = playerCarIdBySession.get(l.sessionUid);
      // If we don't have the session in the window (e.g. outside time range), keep the lap.
      return pcid === undefined || pcid === null || l.carId === pcid;
    });
  }, [lapsQuery.data, playerCarIdBySession]);

  const stats = useMemo(() => {
    const sessions = sessionsQuery.data?.items ?? [];
    const validLaps = playerLaps.filter((l) => l.valid);
    const bestLap = validLaps.length > 0
      ? validLaps.reduce((best, l) => (l.lapTime < best ? l.lapTime : best), validLaps[0]!.lapTime)
      : null;
    const avgMs =
      validLaps.length > 0
        ? validLaps.reduce((acc, l) => acc + l.lapTime, 0) / validLaps.length
        : null;
    return {
      totalSessions: sessionsQuery.data?.total ?? sessions.length,
      totalLaps: playerLaps.length,
      bestLap,
      avgLap: avgMs,
    };
  }, [sessionsQuery.data, playerLaps]);

  const sessionStarts = useMemo(
    () => (sessionsQuery.data?.items ?? []).map((s) => s.startedAt).filter((t): t is string => !!t),
    [sessionsQuery.data],
  );

  // Anchor every chart on "now" when a finite range is active so the rightmost
  // bucket means "this week / this month" — not the most recent data point.
  // For `all`, fall back to the data's max so we don't render empty trailing
  // buckets when the user hasn't recorded anything recently.
  const chartAnchor = range === "all" ? undefined : Date.now();

  const lapBuckets = useMemo(
    () => groupByPlan(playerLaps.map((l) => l.recordedAt), bucketPlan, chartAnchor),
    [playerLaps, bucketPlan, chartAnchor],
  );

  const sessionBuckets = useMemo(
    () => groupByPlan(sessionStarts, bucketPlan, chartAnchor),
    [sessionStarts, bucketPlan, chartAnchor],
  );

  const bucketSub =
    bucketPlan.unit === "week"
      ? `last ${bucketPlan.count} weeks`
      : `last ${bucketPlan.count} months`;

  // Bubble-per-track view. Lap rows don't carry track directly, so we join
  // through the in-range sessions: sessionUid → track. Tracks present in
  // session-options but absent from the join surface as zero-count placeholders
  // so the user can see what they *haven't* practiced lately. NOTE: bounded by
  // the page-size of `lapsQuery` (1000) — for ranges with more laps than that
  // the counts undercount; a dedicated `/laps/aggregate?groupBy=track` endpoint
  // would be the next step.
  const trackPractice = useMemo(() => {
    const allTracks = optionsQuery.data?.tracks ?? [];
    const sessions = sessionsQuery.data?.items ?? [];
    const sessionTrack = new Map<string, string | null>();
    for (const s of sessions) sessionTrack.set(s.uid, s.track);

    const counts = new Map<string, number>();
    for (const t of allTracks) counts.set(t, 0);
    for (const l of playerLaps) {
      const track = sessionTrack.get(l.sessionUid);
      if (!track) continue;
      counts.set(track, (counts.get(track) ?? 0) + 1);
    }
    return Array.from(counts, ([track, count]) => ({ track, count }));
  }, [optionsQuery.data, sessionsQuery.data, playerLaps]);

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
          <SectionHeader title={`Laps per ${bucketPlan.unit}`} sub={bucketSub} />
          <BarChart data={lapBuckets} colorClass="cyan" height={90} />
        </Card>
        <Card>
          <SectionHeader title={`Sessions per ${bucketPlan.unit}`} sub={bucketSub} />
          <BarChart data={sessionBuckets} colorClass="accent" height={90} />
        </Card>
      </div>

      <Card className="mb-6">
        <SectionHeader
          title="Tracks practiced"
          sub="Bubble area ∝ laps in range · dashed = no laps yet"
        />
        <TrackPracticeChart items={trackPractice} />
      </Card>

      <Card className="mb-6">
        <SectionHeader title="All-time best per track" sub="Player's fastest valid lap" />
        <AllTimeBestTable laps={bestPerTrackQuery.data?.items ?? []} />
      </Card>

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
