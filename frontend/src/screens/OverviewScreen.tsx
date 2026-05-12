import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessionOptions, useSessions } from "../api/queries";
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
const ONE_WEEK_MS = 7 * ONE_DAY_MS;

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
 * Bucket `points` into N contiguous time buckets ending at `anchor` and sum each bucket's `value`.
 * Bucket width is fixed for `week` but variable for `month` (calendar months step naturally, so
 * each bucket aligns to the start of a real month).
 *
 * `anchor` defaults to the most recent timestamp so an "all" range hugs the data's actual end
 * instead of rendering empty trailing buckets. Pass `value: 1` to get a count chart.
 */
function bucketize(
  points: { ts: string; value: number }[],
  plan: BucketPlan,
  anchor?: number,
): { label: string; value: number }[] {
  if (points.length === 0) return [];
  const sorted = points
    .map((p) => ({ ms: new Date(p.ts).getTime(), value: p.value }))
    .sort((a, b) => a.ms - b.ms);
  const end = anchor ?? sorted[sorted.length - 1]!.ms;

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
    const value = sorted
      .filter((p) => p.ms >= start && p.ms < next)
      .reduce((acc, p) => acc + p.value, 0);
    const date = new Date(start);
    const label =
      plan.unit === "week"
        ? date.toLocaleDateString("en-GB", { day: "2-digit", month: "short" })
        : date.toLocaleDateString("en-GB", { month: "short", year: "2-digit" });
    return { label, value };
  });
}

export function OverviewScreen() {
  const navigate = useNavigate();
  const sessionsEnabled = useFeatureEnabled("sessions");
  const { fromIso, bucketPlan, range } = useTimeRange();
  const from = fromIso ?? undefined;
  const sessionsQuery = useSessions({ size: 100, sort: "startedAt:DESC", from });
  // playerLap=true so the server-reported `.total` is the right number for the "Total Laps"
  // stat card — no client-side post-filter, no under-count when the in-range count exceeds the
  // page size.
  const lapsQuery = useLaps({ playerLap: true, size: 1000, sort: "lapTime:ASC", from });
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

  // `lapsQuery` already has `playerLap=true` applied server-side, so every item is a player lap.
  const playerLaps = lapsQuery.data?.items ?? [];

  const stats = useMemo(() => {
    const sessions = sessionsQuery.data?.items ?? [];
    // Sum of session driving time (player-car on-track time) over the visible window. Bounded by
    // the sessions page size (100); for ranges with more sessions a server-side aggregate would
    // be the next step.
    const drivingTimeMs = sessions.reduce(
      (acc, s) => acc + (s.drivingTimeMs ?? 0),
      0,
    );
    return {
      totalSessions: sessionsQuery.data?.total ?? sessions.length,
      // Server-reported total respects the `from` + `playerLap=true` filters, so it stays
      // accurate even when the in-range lap count exceeds the page size of `lapsQuery`.
      totalLaps: lapsQuery.data?.total ?? playerLaps.length,
      drivingTimeMs,
    };
  }, [sessionsQuery.data, lapsQuery.data, playerLaps]);

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
      bucketize(
        (sessionsQuery.data?.items ?? [])
          .filter((s): s is typeof s & { startedAt: string } => !!s.startedAt)
          .map((s) => ({ ts: s.startedAt, value: 1 })),
        bucketPlan,
        chartAnchor,
      ),
    [sessionsQuery.data, bucketPlan, chartAnchor],
  );

  const lapBuckets = useMemo(
    () =>
      bucketize(
        playerLaps.map((l) => ({ ts: l.recordedAt, value: 1 })),
        bucketPlan,
        chartAnchor,
      ),
    [playerLaps, bucketPlan, chartAnchor],
  );

  const drivingTimeBuckets = useMemo(
    () =>
      bucketize(
        (sessionsQuery.data?.items ?? [])
          .filter((s): s is typeof s & { startedAt: string } => !!s.startedAt)
          .map((s) => ({ ts: s.startedAt, value: s.drivingTimeMs ?? 0 })),
        bucketPlan,
        chartAnchor,
      ),
    [sessionsQuery.data, bucketPlan, chartAnchor],
  );

  const bucketSub =
    bucketPlan.unit === "week"
      ? `last ${bucketPlan.count} weeks`
      : `last ${bucketPlan.count} months`;

  // Sub-label for the range-bound stat cards. Mirrors the time-range pill's wording so the
  // header, pills and cards all read with the same vocabulary.
  const rangeSub =
    TIME_RANGE_OPTIONS.find((o) => o.key === range)?.sub.toLowerCase() ?? "all time";

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
