import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useState } from "react";
import { useLaps, useSessionOptions, useSessions } from "../api/queries";
import { ErrorState, LoadingState, EmptyState } from "./ui/States";
import { FilterSelect } from "./ui/FilterSelect";
import { formatDate, formatLapTime } from "../lib/format";
const PAGE_SIZE = 20;
/**
 * Filtered + paginated lap list designed for use inside the lap-comparison
 * picker dialog. Mirrors the LapsScreen UX (same filter widgets, same row
 * layout) but in a smaller footprint and with onPick instead of navigation.
 *
 * State is purely local — modal pickers shouldn't pollute the URL.
 */
export function LapBrowser({ defaultTrack, defaultCar, disabledLapUid, onPick }) {
    const optionsQuery = useSessionOptions();
    const [track, setTrack] = useState(defaultTrack);
    const [car, setCar] = useState(defaultCar);
    const [validOnly, setValidOnly] = useState(true);
    const [pbOnly, setPbOnly] = useState(false);
    const [page, setPage] = useState(1);
    // Fetch all sessions so we can show track/car/date next to each lap.
    const sessionsQuery = useSessions({ size: 500, sort: "startedAt:DESC" });
    const sessionsByUid = useMemo(() => {
        const m = new Map();
        for (const s of sessionsQuery.data?.items ?? [])
            m.set(s.uid, s);
        return m;
    }, [sessionsQuery.data]);
    const lapsQuery = useLaps({
        track,
        car,
        page,
        size: PAGE_SIZE,
        sort: "lapTime:ASC",
        validLap: validOnly ? true : undefined,
        personalBest: pbOnly ? true : undefined,
    });
    const onFilterChange = (fn) => {
        fn();
        setPage(1);
    };
    const items = lapsQuery.data?.items ?? [];
    return (_jsxs("div", { className: "flex flex-col gap-4", children: [_jsxs("div", { className: "flex flex-wrap items-end gap-3", children: [_jsx(FilterSelect, { label: "Track", value: track, options: optionsQuery.data?.tracks ?? [], onChange: (v) => onFilterChange(() => setTrack(v)) }), _jsx(FilterSelect, { label: "Car", value: car, options: optionsQuery.data?.cars ?? [], onChange: (v) => onFilterChange(() => setCar(v)) }), _jsx(Toggle, { label: "Valid only", value: validOnly, onChange: (v) => onFilterChange(() => setValidOnly(v)) }), _jsx(Toggle, { label: "Personal bests", value: pbOnly, onChange: (v) => onFilterChange(() => setPbOnly(v)) })] }), _jsx("div", { className: "font-mono text-[11px] text-text-muted", children: lapsQuery.data ? `${lapsQuery.data.total} match · page ${page}` : "—" }), lapsQuery.isLoading && _jsx(LoadingState, {}), lapsQuery.isError && _jsx(ErrorState, { error: lapsQuery.error, onRetry: () => lapsQuery.refetch() }), lapsQuery.data && items.length === 0 && (_jsx(EmptyState, { title: "No laps match", description: "Loosen the filters." })), items.length > 0 && (_jsxs(_Fragment, { children: [_jsxs("div", { className: "overflow-hidden rounded border border-border", children: [_jsxs("div", { className: "grid grid-cols-[60px_120px_1fr_1fr_90px_90px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted", children: [_jsx("div", { children: "#" }), _jsx("div", { children: "Lap time" }), _jsx("div", { children: "Track" }), _jsx("div", { children: "Car" }), _jsx("div", { children: "Date" }), _jsx("div", { children: "Status" })] }), items.map((lap, i) => {
                                const session = sessionsByUid.get(lap.sessionUid);
                                const disabled = disabledLapUid === lap.uid;
                                return (_jsxs("button", { onClick: () => !disabled && onPick(lap.uid), disabled: disabled, title: disabled ? "Already chosen as the other lap" : "Pick this lap", className: [
                                        "grid w-full grid-cols-[60px_120px_1fr_1fr_90px_90px] items-center gap-3 border-b border-border/40 px-3 py-2 text-left last:border-b-0",
                                        disabled
                                            ? "cursor-not-allowed opacity-30"
                                            : "hover:bg-surface-hover",
                                    ].join(" "), children: [_jsx("div", { className: "font-mono text-xs text-text-muted", children: (page - 1) * PAGE_SIZE + i + 1 }), _jsx("div", { className: `font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`, children: formatLapTime(lap.lapTime) }), _jsx("div", { className: "truncate font-sans text-[13px] text-text", children: session?.track ?? _jsx("span", { className: "text-text-dim", children: "unknown" }) }), _jsx("div", { className: "truncate font-sans text-[12px] text-text-muted", children: session?.car ?? _jsx("span", { className: "text-text-dim", children: "unknown" }) }), _jsx("div", { className: "font-mono text-xs text-text-muted", children: formatDate(session?.startedAt) }), _jsxs("div", { className: "font-mono text-[11px]", children: [lap.personalBest && _jsx("span", { className: "text-ok", children: "PB" }), !lap.valid && _jsx("span", { className: "text-accent", children: "INVAL" })] })] }, lap.uid));
                            })] }), _jsxs("div", { className: "flex items-center gap-2", children: [_jsx("button", { disabled: page === 1, onClick: () => setPage((p) => Math.max(1, p - 1)), className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30", children: "Prev" }), _jsx("button", { disabled: !lapsQuery.data || page * PAGE_SIZE >= lapsQuery.data.total, onClick: () => setPage((p) => p + 1), className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30", children: "Next" })] })] }))] }));
}
function Toggle({ label, value, onChange, }) {
    return (_jsxs("button", { onClick: () => onChange(!value), className: [
            "self-end rounded border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors",
            value ? "border-cyan/30 bg-cyan/10 text-cyan" : "border-border text-text-muted hover:bg-surface-hover",
        ].join(" "), children: [value ? "✓" : "○", " ", label] }));
}
