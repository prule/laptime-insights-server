import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useMemo } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useSession, useSessionBestLap, useSessionLaps, useTrackBestLap, } from "../api/queries";
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
        const best = valid.reduce((acc, l) => (acc === null || l.lapTime < acc ? l.lapTime : acc), null);
        const avg = valid.length > 0 ? valid.reduce((s, l) => s + l.lapTime, 0) / valid.length : null;
        return { lapCount: laps.length, validCount: valid.length, best, avg };
    }, [lapsQuery.data]);
    if (sessionQuery.isLoading)
        return _jsx("div", { className: "p-8", children: _jsx(LoadingState, {}) });
    if (sessionQuery.isError)
        return (_jsx("div", { className: "p-8", children: _jsx(ErrorState, { error: sessionQuery.error, onRetry: () => sessionQuery.refetch() }) }));
    if (!sessionQuery.data)
        return _jsx("div", { className: "p-8", children: _jsx(EmptyState, { title: "Session not found" }) });
    const session = sessionQuery.data;
    const laps = lapsQuery.data?.items ?? [];
    const sessionBest = sessionBestQuery.data ?? null;
    const trackBest = trackBestQuery.data ?? null;
    const compareUrl = (lap1, lap2) => `/compare?track=${encodeURIComponent(session.track ?? "")}&lap1=${lap1}&lap2=${lap2}`;
    return (_jsxs("div", { className: "h-full overflow-y-auto px-8 py-7", children: [_jsx("button", { onClick: () => navigate("/sessions"), className: "mb-4 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text", children: "\u2190 Back to sessions" }), _jsx(Card, { className: "mb-4", children: _jsx("div", { className: "flex items-center gap-5", children: _jsxs("div", { className: "flex flex-col gap-1", children: [_jsxs("div", { className: "flex items-center gap-2", children: [_jsx(Badge, { type: session.sessionType }), _jsx("span", { className: "font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted", children: session.simulator })] }), _jsx("div", { className: "font-sans text-2xl font-semibold text-text", children: session.track ?? "Unknown" }), _jsx("div", { className: "font-sans text-sm text-text-muted", children: session.car ?? "Unknown car" }), _jsxs("div", { className: "font-mono text-xs text-text-muted", children: [formatDate(session.startedAt), " \u00B7 ", formatTime(session.startedAt), " \u00B7", " ", formatDuration(session.startedAt, session.endedAt)] })] }) }) }), _jsxs("div", { className: "mb-4 grid grid-cols-4 gap-3", children: [_jsx(StatCard, { label: "Laps", value: stats.lapCount, accent: "cyan", small: true }), _jsx(StatCard, { label: "Valid", value: stats.validCount, accent: "ok", small: true }), _jsx(StatCard, { label: "Best Lap", value: formatLapTime(stats.best), accent: "warn", small: true }), _jsx(StatCard, { label: "Avg Lap", value: formatLapTime(stats.avg), accent: "muted", small: true })] }), _jsxs(Card, { children: [_jsx(SectionHeader, { title: "Laps", action: "Trend", sub: laps.length > 0 ? (_jsx("span", { className: "text-text-dim", children: _jsx(Sparkline, { values: laps.filter((l) => l.valid).map((l) => l.lapTime), color: "#00d4ff" }) })) : undefined }), lapsQuery.isLoading && _jsx(LoadingState, {}), lapsQuery.isError && (_jsx(ErrorState, { error: lapsQuery.error, onRetry: () => lapsQuery.refetch() })), lapsQuery.data && laps.length === 0 && _jsx(EmptyState, { title: "No laps recorded" }), laps.length > 0 && (_jsxs("div", { className: "overflow-hidden rounded border border-border", children: [_jsxs("div", { className: "grid grid-cols-[60px_120px_120px_100px_90px_200px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted", children: [_jsx("div", { children: "Lap" }), _jsx("div", { children: "Recorded" }), _jsx("div", { children: "Lap time" }), _jsx("div", { children: "\u0394 to best" }), _jsx("div", { children: "Status" }), _jsx("div", { children: "Compare" })] }), laps.map((lap) => (_jsx(LapRow, { lap: lap, bestSoFar: stats.best, sessionBest: sessionBest, trackBest: trackBest, onCompare: (other) => navigate(compareUrl(lap.uid, other)) }, lap.uid)))] }))] })] }));
}
function LapRow({ lap, bestSoFar, sessionBest, trackBest, onCompare, }) {
    // "vs session best" — only meaningful for valid laps that aren't already the
    // session's best. "vs track PB" — same rule, plus the track PB must exist
    // and (rarely) might be this very lap.
    const sessionBestUid = sessionBest?.uid;
    const trackBestUid = trackBest?.uid;
    const canVsSessionBest = lap.valid && !!sessionBestUid && sessionBestUid !== lap.uid;
    const canVsTrackBest = lap.valid && !!trackBestUid && trackBestUid !== lap.uid;
    return (_jsxs("div", { className: "grid grid-cols-[60px_120px_120px_100px_90px_200px] items-center gap-3 border-b border-border/40 px-3 py-2 last:border-b-0 hover:bg-surface-hover", children: [_jsxs("div", { className: "font-mono text-xs text-text-muted", children: ["#", lap.lapNumber] }), _jsx("div", { className: "font-mono text-xs text-text-muted", children: formatTime(lap.recordedAt) }), _jsx("div", { className: `font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`, children: lap.valid ? formatLapTime(lap.lapTime) : _jsx("span", { className: "text-text-dim", children: "INVAL" }) }), _jsx("div", { children: lap.valid ? (_jsx(Delta, { ms: lap.lapTime, referenceMs: bestSoFar })) : (_jsx("span", { className: "text-text-dim", children: "\u2014" })) }), _jsxs("div", { className: "font-mono text-[11px]", children: [lap.personalBest && _jsx("span", { className: "text-ok", children: "PB" }), !lap.valid && _jsx("span", { className: "text-accent", children: "INVALID" })] }), _jsxs("div", { className: "flex gap-1", children: [_jsx(CompareButton, { label: "vs best", title: !canVsSessionBest && lap.uid === sessionBestUid
                            ? "This lap is the session's best"
                            : "Compare against this session's fastest valid lap", enabled: canVsSessionBest, onClick: () => sessionBestUid && onCompare(sessionBestUid) }), _jsx(CompareButton, { label: "vs PB", title: !canVsTrackBest && lap.uid === trackBestUid
                            ? "This lap is the track PB"
                            : "Compare against the all-time fastest valid lap at this track", enabled: canVsTrackBest, onClick: () => trackBestUid && onCompare(trackBestUid) })] })] }));
}
function CompareButton({ label, title, enabled, onClick, }) {
    return (_jsx("button", { onClick: onClick, disabled: !enabled, title: title, className: "rounded border border-border px-2 py-1 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted transition-colors hover:border-cyan/40 hover:text-cyan disabled:cursor-not-allowed disabled:opacity-30 disabled:hover:border-border disabled:hover:text-text-muted", children: label }));
}
