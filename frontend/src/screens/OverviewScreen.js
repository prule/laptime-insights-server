import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessions } from "../api/queries";
import { useTimeRange } from "../providers/TimeRangeProvider";
import { Card } from "../components/ui/Card";
import { SectionHeader } from "../components/ui/SectionHeader";
import { StatCard } from "../components/ui/StatCard";
import { BarChart } from "../components/ui/BarChart";
import { ErrorState, LoadingState } from "../components/ui/States";
import { SessionRow } from "../components/SessionRow";
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
function groupByPlan(timestamps, plan, anchor) {
    if (timestamps.length === 0)
        return [];
    const sortedMs = timestamps.map((t) => new Date(t).getTime()).sort((a, b) => a - b);
    const end = anchor ?? sortedMs[sortedMs.length - 1];
    const starts = [];
    if (plan.unit === "week") {
        for (let i = plan.count - 1; i >= 0; i--) {
            starts.push(end - i * ONE_WEEK_MS);
        }
    }
    else {
        // Walk back month-by-month from the anchor's month start.
        const anchorDate = new Date(end);
        const baseYear = anchorDate.getFullYear();
        const baseMonth = anchorDate.getMonth();
        for (let i = plan.count - 1; i >= 0; i--) {
            starts.push(new Date(baseYear, baseMonth - i, 1).getTime());
        }
    }
    return starts.map((start, i) => {
        const next = i < starts.length - 1 ? starts[i + 1] : end + ONE_DAY_MS;
        const count = sortedMs.filter((t) => t >= start && t < next).length;
        const date = new Date(start);
        const label = plan.unit === "week"
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
    const stats = useMemo(() => {
        const sessions = sessionsQuery.data?.items ?? [];
        const laps = lapsQuery.data?.items ?? [];
        const validLaps = laps.filter((l) => l.valid);
        const bestLap = validLaps[0]?.lapTime ?? null;
        const avgMs = validLaps.length > 0
            ? validLaps.reduce((acc, l) => acc + l.lapTime, 0) / validLaps.length
            : null;
        return {
            totalSessions: sessionsQuery.data?.total ?? sessions.length,
            totalLaps: lapsQuery.data?.total ?? laps.length,
            bestLap,
            avgLap: avgMs,
        };
    }, [sessionsQuery.data, lapsQuery.data]);
    const sessionStarts = useMemo(() => (sessionsQuery.data?.items ?? []).map((s) => s.startedAt).filter((t) => !!t), [sessionsQuery.data]);
    // Anchor every chart on "now" when a finite range is active so the rightmost
    // bucket means "this week / this month" — not the most recent data point.
    // For `all`, fall back to the data's max so we don't render empty trailing
    // buckets when the user hasn't recorded anything recently.
    const chartAnchor = range === "all" ? undefined : Date.now();
    const lapBuckets = useMemo(() => groupByPlan((lapsQuery.data?.items ?? []).map((l) => l.recordedAt), bucketPlan, chartAnchor), [lapsQuery.data, bucketPlan, chartAnchor]);
    const sessionBuckets = useMemo(() => groupByPlan(sessionStarts, bucketPlan, chartAnchor), [sessionStarts, bucketPlan, chartAnchor]);
    const bucketSub = bucketPlan.unit === "week"
        ? `last ${bucketPlan.count} weeks`
        : `last ${bucketPlan.count} months`;
    if (sessionsQuery.isLoading || lapsQuery.isLoading) {
        return _jsx("div", { className: "p-8", children: _jsx(LoadingState, {}) });
    }
    if (sessionsQuery.isError) {
        return (_jsx("div", { className: "p-8", children: _jsx(ErrorState, { error: sessionsQuery.error, onRetry: () => sessionsQuery.refetch() }) }));
    }
    const recentSessions = (sessionsQuery.data?.items ?? []).slice(0, 4);
    return (_jsxs("div", { className: "h-full overflow-y-auto px-8 py-7", children: [_jsxs("div", { className: "mb-7 flex items-center gap-5 border-b border-border pb-6", children: [_jsx("div", { className: "flex h-[52px] w-[52px] items-center justify-center rounded-[10px] border border-accent/[0.3] bg-gradient-to-br from-accent/20 to-accent/10 font-mono text-lg font-bold text-accent", children: "\u25C8" }), _jsxs("div", { children: [_jsx("div", { className: "font-sans text-xl font-semibold text-text", children: "LapTime Insights" }), _jsxs("div", { className: "font-sans text-[13px] text-text-muted", children: ["Telemetry across ", stats.totalSessions, " sessions"] })] }), _jsxs("div", { className: "ml-auto text-right", children: [_jsx("div", { className: "font-mono text-[11px] tracking-[0.08em] text-text-muted", children: "PERSONAL BEST" }), _jsx("div", { className: "font-mono text-2xl font-bold text-ok", children: formatLapTime(stats.bestLap) })] })] }), _jsxs("div", { className: "mb-6 grid grid-cols-4 gap-3", children: [_jsx(StatCard, { label: "Total Sessions", value: formatNumber(stats.totalSessions), accent: "cyan", sub: "all-time" }), _jsx(StatCard, { label: "Total Laps", value: formatNumber(stats.totalLaps), accent: "accent", sub: "all-time" }), _jsx(StatCard, { label: "Best Lap", value: formatLapTime(stats.bestLap), accent: "ok" }), _jsx(StatCard, { label: "Avg Lap", value: formatLapTime(stats.avgLap), accent: "warn", sub: "across valid laps" })] }), _jsxs("div", { className: "mb-6 grid grid-cols-2 gap-4", children: [_jsxs(Card, { children: [_jsx(SectionHeader, { title: `Laps per ${bucketPlan.unit}`, sub: bucketSub }), _jsx(BarChart, { data: lapBuckets, colorClass: "cyan", height: 90 })] }), _jsxs(Card, { children: [_jsx(SectionHeader, { title: `Sessions per ${bucketPlan.unit}`, sub: bucketSub }), _jsx(BarChart, { data: sessionBuckets, colorClass: "accent", height: 90 })] })] }), _jsxs(Card, { children: [_jsx(SectionHeader, { title: "Recent sessions", action: "View all", onAction: () => navigate("/sessions") }), _jsx("div", { className: "flex flex-col gap-1", children: recentSessions.map((s) => (_jsx(SessionRow, { session: s }, s.uid))) })] })] }));
}
