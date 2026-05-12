import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import {
  useLapAggregate,
  useLaps,
  useSessionAggregate,
  useSessionOptions,
  useSessions,
} from "../api/queries";
import { useFeatureEnabled } from "../providers/FeaturesProvider";
import {
  type BucketPlan,
  TIME_RANGE_OPTIONS,
  useTimeRange,
} from "../providers/TimeRangeProvider";
import { Card } from "../components/ui/Card";
import { SectionHeader } from "../components/ui/SectionHeader";
import { StatCard } from "../components/ui/StatCard";
import { BarChart } from "../components/ui/BarChart";
import { ErrorState, LoadingState } from "../components/ui/States";
import { SessionRow } from "../components/SessionRow";
import { TrackPracticeChart } from "../components/ui/TrackPracticeChart";
import { AllTimeBestTable } from "../components/AllTimeBestTable";
import { formatDrivingTime, formatNumber } from "../lib/format";

const ONE_DAY_MS = 86_400_000;

/**
 * Local-calendar key (YYYY-MM-DD) so two timestamps on the same day collapse to one streak entry
 * regardless of time-of-day. Uses local timezone — a session that starts late at night counts on
 * the day the player perceives it as.
 */
function dayKey(ms: number): string {
  const d = new Date(ms);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function parseDayKey(key: string): Date {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y!, (m ?? 1) - 1, d ?? 1);
}

/**
 * Number of consecutive calendar days ending at the most recent session day. `lastDate` is the
 * most recent day with a session (or null when there are no sessions). The header decides whether
 * the streak is "live" (last day is today or yesterday) or "ended" based on `lastDate`.
 */
function computeStreak(timestamps: string[]): { days: number; lastDate: Date | null } {
  if (timestamps.length === 0) return { days: 0, lastDate: null };
  const keys = Array.from(new Set(timestamps.map((t) => dayKey(new Date(t).getTime())))).sort();
  const desc = keys.slice().reverse();
  const lastDate = parseDayKey(desc[0]!);
  let streak = 1;
  let prev = lastDate;
  for (let i = 1; i < desc.length; i++) {
    const cur = parseDayKey(desc[i]!);
    const diffDays = Math.round((prev.getTime() - cur.getTime()) / ONE_DAY_MS);
    if (diffDays === 1) {
      streak++;
      prev = cur;
    } else {
      break;
    }
  }
  return { days: streak, lastDate };
}

/**
 * Build the N bucket start dates the dashboard chart layout uses, ending at the calendar bucket
 * containing `anchor`. Calendar-aligned: weeks start on Monday, months on the 1st. Returned in
 * chronological order. Joins onto aggregate-API responses by matching `bucketStartKey`.
 */
function bucketStarts(plan: BucketPlan, anchor: number): Date[] {
  const anchorDate = new Date(anchor);
  const starts: Date[] = [];
  if (plan.unit === "week") {
    const dayIndex = (anchorDate.getDay() + 6) % 7; // 0 = Monday
    const anchorMonday = new Date(
      anchorDate.getFullYear(),
      anchorDate.getMonth(),
      anchorDate.getDate() - dayIndex,
    );
    for (let i = plan.count - 1; i >= 0; i--) {
      const start = new Date(anchorMonday);
      start.setDate(start.getDate() - i * 7);
      starts.push(start);
    }
  } else {
    const baseYear = anchorDate.getFullYear();
    const baseMonth = anchorDate.getMonth();
    for (let i = plan.count - 1; i >= 0; i--) {
      starts.push(new Date(baseYear, baseMonth - i, 1));
    }
  }
  return starts;
}

/**
 * Bar-chart x-axis labels. Kept short so they fit in the 3-column dashboard layout — full date
 * context is exposed on hover via the chart's `title` tooltip. Weeks show the day of month;
 * months show the 3-letter month name.
 */
function bucketLabel(start: Date, unit: BucketPlan["unit"]): string {
  return unit === "week"
    ? start.toLocaleDateString("en-GB", { day: "2-digit" })
    : start.toLocaleDateString("en-GB", { month: "short" });
}

/**
 * Hover-tooltip text for a bucket: the full human-readable date so the reader can disambiguate
 * the terse on-screen label. Format mirrors the format we use elsewhere in the UI ("Mon, 06 Apr
 * 2026" / "Apr 2026").
 */
function bucketTitle(start: Date, unit: BucketPlan["unit"]): string {
  return unit === "week"
    ? `Week of ${start.toLocaleDateString("en-GB", {
        weekday: "short",
        day: "2-digit",
        month: "short",
        year: "numeric",
      })}`
    : start.toLocaleDateString("en-GB", { month: "long", year: "numeric" });
}

/**
 * Aggregate-API bucket key (matches the backend's `formatBucketKey`). `YYYY-MM-DD` for week
 * (Monday) and day, `YYYY-MM` for month. Used to join the sparse aggregate response onto the
 * dense `bucketStarts` grid.
 */
function bucketStartKey(start: Date, unit: BucketPlan["unit"]): string {
  const y = start.getFullYear();
  const m = String(start.getMonth() + 1).padStart(2, "0");
  if (unit === "month") return `${y}-${m}`;
  const d = String(start.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/**
 * Drop the sparse `buckets` from an aggregate endpoint onto the dense `bucketStarts` grid.
 * Missing buckets become `value: 0` so the chart renders gaps as empty bars. `getValue` selects
 * which metric to chart — e.g. `b => b.count` or `b => b.drivingTimeMs`.
 */
function alignAggregate<B extends { key: string }>(
  buckets: readonly B[] | undefined,
  plan: BucketPlan,
  anchor: number,
  getValue: (b: B) => number,
): { label: string; title: string; value: number }[] {
  const lookup = new Map((buckets ?? []).map((b) => [b.key, getValue(b)]));
  return bucketStarts(plan, anchor).map((start) => ({
    label: bucketLabel(start, plan.unit),
    title: bucketTitle(start, plan.unit),
    value: lookup.get(bucketStartKey(start, plan.unit)) ?? 0,
  }));
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

  const stats = useMemo(() => {
    // Server-aggregated sum across buckets — accurate regardless of session-page size.
    const drivingTimeMs = (sessionAggQuery.data?.buckets ?? []).reduce(
      (acc, b) => acc + b.drivingTimeMs,
      0,
    );
    return {
      // Server-reported totals from the count-only queries — `size: 1` for laps; sessionsQuery
      // already supplies `.total` from its own pagination.
      totalSessions:
        sessionsQuery.data?.total ?? sessionsQuery.data?.items.length ?? 0,
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
    if (days === 0 || !lastDate) return { days: 0, live: false, lastLabel: null as string | null };
    const today = new Date();
    const todayMs = new Date(today.getFullYear(), today.getMonth(), today.getDate()).getTime();
    const diffDays = Math.round((todayMs - lastDate.getTime()) / ONE_DAY_MS);
    const live = diffDays <= 1; // today or yesterday → still alive
    const lastLabel =
      diffDays === 0
        ? "today"
        : diffDays === 1
          ? "yesterday"
          : `ended ${lastDate.toLocaleDateString("en-GB", { day: "2-digit", month: "short" })}`;
    return { days, live, lastLabel };
  }, [sessionsQuery.data]);

  // Anchor every chart on "now" when a finite range is active so the rightmost
  // bucket means "this week / this month" — not the most recent data point.
  // For `all`, fall back to the data's max so we don't render empty trailing
  // buckets when the user hasn't recorded anything recently.
  const chartAnchor = range === "all" ? undefined : Date.now();

  const sessionBuckets = useMemo(
    () =>
      alignAggregate(
        sessionAggQuery.data?.buckets,
        bucketPlan,
        chartAnchor ?? Date.now(),
        (b) => b.count,
      ),
    [sessionAggQuery.data, bucketPlan, chartAnchor],
  );

  // Aggregate response lands the count on the same calendar grid the chart renders, so the join
  // is a simple key lookup. Missing buckets render as zero.
  const lapBuckets = useMemo(
    () =>
      alignAggregate(
        lapsByBucketQuery.data?.buckets,
        bucketPlan,
        chartAnchor ?? Date.now(),
        (b) => b.count,
      ),
    [lapsByBucketQuery.data, bucketPlan, chartAnchor],
  );

  const drivingTimeBuckets = useMemo(
    () =>
      alignAggregate(
        sessionAggQuery.data?.buckets,
        bucketPlan,
        chartAnchor ?? Date.now(),
        (b) => b.drivingTimeMs,
      ),
    [sessionAggQuery.data, bucketPlan, chartAnchor],
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

  if (sessionsQuery.isLoading || lapsByBucketQuery.isLoading) {
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
        <StatCard label="Total Sessions" value={formatNumber(stats.totalSessions)} accent="cyan" sub={rangeSub} />
        <StatCard label="Total Laps" value={formatNumber(stats.totalLaps)} accent="accent" sub={rangeSub} />
        <StatCard label="Driving Time" value={formatDrivingTime(stats.drivingTimeMs)} accent="warn" sub={rangeSub} />
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
        <AllTimeBestTable laps={bestPerTrackQuery.data?.items ?? []} />
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
