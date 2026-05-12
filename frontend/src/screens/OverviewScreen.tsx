import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import type { UseQueryResult } from "@tanstack/react-query";
import {
  useLapAggregate,
  useLaps,
  useSessionAggregate,
  useSessionOptions,
  useSessions,
} from "../api/queries";
import { useFeatureEnabled } from "../providers/FeaturesProvider";
import { TIME_RANGE_OPTIONS, useTimeRange } from "../providers/TimeRangeProvider";
import { Card } from "../components/ui/Card";
import { SectionHeader } from "../components/ui/SectionHeader";
import { StatCard } from "../components/ui/StatCard";
import { BarChart } from "../components/ui/BarChart";
import { ErrorState, LoadingState } from "../components/ui/States";
import { SessionRow } from "../components/SessionRow";
import { TrackPracticeChart } from "../components/ui/TrackPracticeChart";
import { AllTimeBestTable } from "../components/AllTimeBestTable";
import { formatDrivingTime, formatNumber } from "../lib/format";
import { alignAggregate, latestBucketDate } from "../lib/buckets";
import { computeStreak, describeStreakFreshness } from "../lib/streak";

/** Reduces a list of TanStack queries down to a single combined loading / first-error view. */
function combineQueryState(queries: UseQueryResult<unknown>[]): {
  isLoading: boolean;
  error: unknown | null;
} {
  return {
    isLoading: queries.some((q) => q.isLoading),
    error: queries.find((q) => q.isError)?.error ?? null,
  };
}

export function OverviewScreen() {
  const navigate = useNavigate();
  const sessionsEnabled = useFeatureEnabled("sessions");
  const { fromIso, bucketPlan, range } = useTimeRange();
  const from = fromIso ?? undefined;
  // Recent sessions list + streak both want raw session rows. Stat-card totals and the activity
  // charts use the aggregate endpoints below.
  const sessionsQuery = useSessions({ size: 100, sort: "startedAt:DESC", from });
  // Server-aggregated per-bucket session count + summed driving time. Single request drives both
  // the "Sessions per …" and "Driving time per …" charts and the Driving Time stat card.
  const sessionAggQuery = useSessionAggregate({ groupBy: bucketPlan.unit, from });
  // Tiny count-only query for the "Total Laps" stat card — `size: 1` so the server still computes
  // `.total` but we don't pay the cost of transferring rows we'd ignore.
  const lapsCountQuery = useLaps({ playerLap: true, size: 1, from });
  // Server-aggregated per-bucket lap count for the activity chart. The `groupBy` matches the
  // dashboard's bucket plan so the response keys land exactly on the chart's x-axis grid.
  const lapsByBucketQuery = useLapAggregate({
    groupBy: bucketPlan.unit,
    playerLap: true,
    from,
  });
  // Server-aggregated per-track lap count for the "Tracks practiced" bubble chart.
  const lapsByTrackQuery = useLapAggregate({
    groupBy: "track",
    playerLap: true,
    from,
  });
  // All-time best lap per track for the player. Independent of the time-range filter — "all-time"
  // means all-time, regardless of the dashboard window.
  const bestPerTrackQuery = useLaps({
    playerLap: true,
    validLap: true,
    allTimeBest: true,
    size: 100,
    sort: "track:ASC",
  });
  // Full universe of tracks the user has *ever* visited — feeds the practice chart so tracks with
  // zero laps in range still render as "haven't been here lately" placeholders. Not bound to the
  // active range on purpose.
  const optionsQuery = useSessionOptions();

  // Single combined loading + error gate so we don't render half-populated UI (zeroed Driving
  // Time, empty Tracks bubble, "no laps recorded yet") while late-arriving queries load.
  const { isLoading, error } = combineQueryState([
    sessionsQuery,
    sessionAggQuery,
    lapsCountQuery,
    lapsByBucketQuery,
    lapsByTrackQuery,
    optionsQuery,
  ]);

  // Anchor the activity charts on "now" for finite ranges so the rightmost bar means "this
  // week/month". For `all`, anchor on the latest bucket actually present in the data so a long
  // pause since the last session doesn't render trailing empty buckets up to today. Falls back to
  // today when there's no data yet.
  const anchorMs = useMemo(() => {
    if (range !== "all") return Date.now();
    const latest = latestBucketDate(sessionAggQuery.data?.buckets, bucketPlan.unit);
    return (latest ?? new Date()).getTime();
  }, [range, sessionAggQuery.data, bucketPlan.unit]);

  const stats = useMemo(() => {
    // Server-aggregated sum across buckets — accurate regardless of session-page size.
    const drivingTimeMs = (sessionAggQuery.data?.buckets ?? []).reduce(
      (acc, b) => acc + b.drivingTimeMs,
      0,
    );
    return {
      totalSessions: sessionsQuery.data?.total ?? 0,
      totalLaps: lapsCountQuery.data?.total ?? 0,
      drivingTimeMs,
    };
  }, [sessionsQuery.data, sessionAggQuery.data, lapsCountQuery.data]);

  // Streak is a global property of the activity timeline, intentionally not bound to the time
  // range filter — a streak truncated by the range would be misleading.
  const streak = useMemo(() => {
    const timestamps = (sessionsQuery.data?.items ?? [])
      .map((s) => s.startedAt)
      .filter((t): t is string => !!t);
    const { days, lastDate } = computeStreak(timestamps);
    const { live, lastLabel } = describeStreakFreshness(lastDate);
    return { days, live, lastLabel };
  }, [sessionsQuery.data]);

  const sessionBuckets = useMemo(
    () => alignAggregate(sessionAggQuery.data?.buckets, bucketPlan, anchorMs, (b) => b.count),
    [sessionAggQuery.data, bucketPlan, anchorMs],
  );

  const lapBuckets = useMemo(
    () => alignAggregate(lapsByBucketQuery.data?.buckets, bucketPlan, anchorMs, (b) => b.count),
    [lapsByBucketQuery.data, bucketPlan, anchorMs],
  );

  const drivingTimeBuckets = useMemo(
    () =>
      alignAggregate(sessionAggQuery.data?.buckets, bucketPlan, anchorMs, (b) => b.drivingTimeMs),
    [sessionAggQuery.data, bucketPlan, anchorMs],
  );

  const bucketSub =
    bucketPlan.unit === "week"
      ? `last ${bucketPlan.count} weeks`
      : `last ${bucketPlan.count} months`;

  // Sub-label for the range-bound stat cards. Mirrors the time-range pill's wording so the
  // header, pills and cards all read with the same vocabulary.
  const rangeSub =
    TIME_RANGE_OPTIONS.find((o) => o.key === range)?.sub.toLowerCase() ?? "all time";

  // Bubble-per-track view. Aggregate response gives one count per track in-range; the union with
  // `optionsQuery.tracks` keeps zero-count placeholders for tracks the player has visited at some
  // point but not within the active range — useful "haven't been back here lately" signal.
  const trackPractice = useMemo(() => {
    const allTracks = optionsQuery.data?.tracks ?? [];
    const counts = new Map<string, number>();
    for (const t of allTracks) counts.set(t, 0);
    for (const b of lapsByTrackQuery.data?.buckets ?? []) {
      counts.set(b.key, b.count);
    }
    return Array.from(counts, ([track, count]) => ({ track, count }));
  }, [optionsQuery.data, lapsByTrackQuery.data]);

  if (isLoading) {
    return (
      <div className="p-8">
        <LoadingState />
      </div>
    );
  }
  if (error) {
    return (
      <div className="p-8">
        <ErrorState error={error} onRetry={() => sessionsQuery.refetch()} />
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
          <div className="font-mono text-[11px] tracking-[0.08em] text-text-muted">STREAK</div>
          <div
            className={`font-mono text-2xl font-bold ${streak.live ? "text-ok" : "text-text-muted"}`}
          >
            {streak.days > 0 ? `${streak.days}d` : "—"}
          </div>
          {streak.lastLabel && (
            <div className="font-sans text-[11px] text-text-muted">{streak.lastLabel}</div>
          )}
        </div>
      </div>

      <div className="mb-6 grid grid-cols-3 gap-3">
        <StatCard
          label="Total Sessions"
          value={formatNumber(stats.totalSessions)}
          accent="cyan"
          sub={rangeSub}
        />
        <StatCard
          label="Total Laps"
          value={formatNumber(stats.totalLaps)}
          accent="accent"
          sub={rangeSub}
        />
        <StatCard
          label="Driving Time"
          value={formatDrivingTime(stats.drivingTimeMs)}
          accent="warn"
          sub={rangeSub}
        />
      </div>

      <div className="mb-6 grid grid-cols-3 gap-4">
        <Card>
          <SectionHeader title={`Sessions per ${bucketPlan.unit}`} sub={bucketSub} />
          <BarChart data={sessionBuckets} colorClass="cyan" height={90} />
        </Card>
        <Card>
          <SectionHeader title={`Laps per ${bucketPlan.unit}`} sub={bucketSub} />
          <BarChart data={lapBuckets} colorClass="accent" height={90} />
        </Card>
        <Card>
          <SectionHeader title={`Driving time per ${bucketPlan.unit}`} sub={bucketSub} />
          <BarChart data={drivingTimeBuckets} colorClass="warn" height={90} />
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
        <AllTimeBestTable
          laps={bestPerTrackQuery.data?.items ?? []}
          isLoading={bestPerTrackQuery.isLoading}
        />
      </Card>

      <Card>
        <SectionHeader
          title="Recent sessions"
          action={sessionsEnabled ? "View all" : undefined}
          onAction={sessionsEnabled ? () => navigate("/sessions") : undefined}
        />
        <div className="flex flex-col gap-1">
          {recentSessions.map((s) => (
            <SessionRow key={s.uid} session={s} />
          ))}
        </div>
      </Card>
    </div>
  );
}
