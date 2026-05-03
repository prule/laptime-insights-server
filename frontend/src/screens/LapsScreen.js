import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessionOptions, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import { formatLapTime, formatTime } from "../lib/format";
import { getBool, getInt, getString, useUrlState } from "../hooks/useUrlState";
const PAGE_SIZE = 50;
/**
 * Lap-search screen.
 *
 * Filters and pagination live in the URL querystring (e.g.
 * `/laps?track=Monza&validOnly=true&page=2`) — reload-safe and shareable.
 *
 * Includes a "select mode" so users can pick exactly two laps from the
 * filtered, paginated list and jump straight into the compare screen.
 * Selection is local component state — the URL only carries filter state.
 */
export function LapsScreen() {
    const navigate = useNavigate();
    const [params, setParam, setMany] = useUrlState();
    const facets = {
        track: getString(params, "track"),
        car: getString(params, "car"),
        simulator: getString(params, "simulator"),
    };
    const showInvalid = getBool(params, "invalid", false);
    const validOnly = !showInvalid;
    const pbOnly = getBool(params, "pb", false);
    const page = getInt(params, "page", 1);
    const facetsActive = !!(facets.track || facets.car || facets.simulator);
    // Multi-select state for compare. We deliberately keep this in component
    // state, not the URL — selection is a transient pre-action, not a view.
    const [selectMode, setSelectMode] = useState(false);
    const [selected, setSelected] = useState([]);
    const optionsQuery = useSessionOptions();
    const sessionsQuery = useSessions({ size: 500, sort: "startedAt:DESC" });
    const lapsQuery = useLaps({
        page,
        size: PAGE_SIZE,
        sort: "lapTime:ASC",
        validLap: validOnly ? true : undefined,
        personalBest: pbOnly ? true : undefined,
        car: facets.car,
        track: facets.track,
        simulator: facets.simulator,
    });
    const sessionsByUid = useMemo(() => {
        const map = new Map();
        for (const s of sessionsQuery.data?.items ?? [])
            map.set(s.uid, s);
        return map;
    }, [sessionsQuery.data]);
    const items = lapsQuery.data?.items ?? [];
    const updateFacet = (key, value) => {
        setMany({ [key]: value, page: undefined });
    };
    const toggleSelect = (lapUid) => {
        setSelected((prev) => {
            if (prev.includes(lapUid))
                return prev.filter((u) => u !== lapUid);
            // Cap at 2: drop the older selection so the most recent two clicks win.
            if (prev.length >= 2)
                return [prev[1], lapUid];
            return [...prev, lapUid];
        });
    };
    const compareSelected = () => {
        if (selected.length !== 2)
            return;
        const [lap1, lap2] = selected;
        // Track is preserved in URL only when the user picked one — otherwise
        // /compare infers it from the chosen laps.
        const trackParam = facets.track ? `&track=${encodeURIComponent(facets.track)}` : "";
        navigate(`/compare?lap1=${lap1}&lap2=${lap2}${trackParam}`);
    };
    return (_jsxs("div", { className: "h-full overflow-y-auto px-8 py-7", children: [_jsxs(Card, { className: "mb-4", children: [_jsx(SectionHeader, { title: "Filter", sub: "All filters hit /api/1/laps directly \u00B7 state is mirrored to the URL" }), _jsxs("div", { className: "flex flex-wrap items-end gap-3", children: [_jsx(FilterSelect, { label: "Track", value: facets.track, options: optionsQuery.data?.tracks ?? [], onChange: (v) => updateFacet("track", v) }), _jsx(FilterSelect, { label: "Car", value: facets.car, options: optionsQuery.data?.cars ?? [], onChange: (v) => updateFacet("car", v) }), _jsx(FilterSelect, { label: "Simulator", value: facets.simulator, options: optionsQuery.data?.simulators ?? [], onChange: (v) => updateFacet("simulator", v) }), _jsx(Toggle, { label: "Valid only", value: validOnly, onChange: (v) => setMany({ invalid: v ? undefined : true, page: undefined }) }), _jsx(Toggle, { label: "Personal bests", value: pbOnly, onChange: (v) => setMany({ pb: v ? true : undefined, page: undefined }) }), (facetsActive || pbOnly || !validOnly) && (_jsx("button", { onClick: () => setMany({
                                    track: undefined,
                                    car: undefined,
                                    simulator: undefined,
                                    invalid: undefined,
                                    pb: undefined,
                                    page: undefined,
                                }), className: "self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text", children: "Reset" }))] })] }), _jsxs(Card, { children: [_jsxs("div", { className: "mb-4 flex items-baseline justify-between", children: [_jsxs("div", { children: [_jsx("div", { className: "font-sans text-sm font-medium text-text", children: "Fastest laps" }), _jsxs("div", { className: "font-sans text-xs text-text-muted", children: [lapsQuery.data ? `${lapsQuery.data.total} match · page ${page}` : "—", selectMode && (_jsxs("span", { className: "ml-2 text-cyan", children: ["\u00B7 select mode: ", selected.length, "/2 picked"] }))] })] }), _jsx("div", { className: "flex items-center gap-2", children: selectMode ? (_jsxs(_Fragment, { children: [_jsx("button", { onClick: compareSelected, disabled: selected.length !== 2, className: "rounded border border-cyan/40 bg-cyan/10 px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-cyan transition-colors disabled:cursor-not-allowed disabled:opacity-30", children: "Compare selected" }), _jsx("button", { onClick: () => {
                                                setSelected([]);
                                                setSelectMode(false);
                                            }, className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text", children: "Cancel" })] })) : (_jsx("button", { onClick: () => setSelectMode(true), className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:border-cyan/40 hover:text-cyan", children: "Select to compare" })) })] }), lapsQuery.isLoading && _jsx(LoadingState, {}), lapsQuery.isError && (_jsx(ErrorState, { error: lapsQuery.error, onRetry: () => lapsQuery.refetch() })), lapsQuery.data && items.length === 0 && (_jsx(EmptyState, { title: "No laps match", description: facetsActive ? "Try clearing the car/track/simulator filters." : "Loosen the toggles or seed the database." })), items.length > 0 && (_jsxs(_Fragment, { children: [_jsxs("div", { className: "overflow-hidden rounded border border-border", children: [_jsx(Header, { selectMode: selectMode }), items.map((lap, i) => {
                                        const session = sessionsByUid.get(lap.sessionUid);
                                        const isSelected = selected.includes(lap.uid);
                                        return (_jsx(LapRow, { index: (page - 1) * PAGE_SIZE + i + 1, lap: lap, session: session, selectMode: selectMode, selected: isSelected, onToggleSelect: () => toggleSelect(lap.uid), onOpen: () => navigate(`/sessions/${lap.sessionUid}`) }, lap.uid));
                                    })] }), _jsxs("div", { className: "mt-3 flex items-center gap-2", children: [_jsx("button", { disabled: page === 1, onClick: () => setParam("page", page > 2 ? page - 1 : undefined), className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30", children: "Prev" }), _jsx("button", { disabled: !lapsQuery.data || page * PAGE_SIZE >= lapsQuery.data.total, onClick: () => setParam("page", page + 1), className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30", children: "Next" })] })] }))] })] }));
}
const COL_TEMPLATE_NORMAL = "grid-cols-[50px_120px_1fr_1fr_90px_110px_70px]";
const COL_TEMPLATE_SELECT = "grid-cols-[36px_50px_120px_1fr_1fr_90px_110px_70px]";
function Header({ selectMode }) {
    return (_jsxs("div", { className: `grid items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted ${selectMode ? COL_TEMPLATE_SELECT : COL_TEMPLATE_NORMAL}`, children: [selectMode && _jsx("div", {}), _jsx("div", { children: "#" }), _jsx("div", { children: "Lap time" }), _jsx("div", { children: "Track" }), _jsx("div", { children: "Car" }), _jsx("div", { children: "Sim" }), _jsx("div", { children: "Recorded" }), _jsx("div", { children: "Status" })] }));
}
function LapRow({ index, lap, session, selectMode, selected, onToggleSelect, onOpen, }) {
    const onClick = selectMode ? onToggleSelect : onOpen;
    return (_jsxs("button", { onClick: onClick, className: [
            "grid w-full items-center gap-3 border-b border-border/40 px-3 py-2 text-left last:border-b-0",
            selectMode ? COL_TEMPLATE_SELECT : COL_TEMPLATE_NORMAL,
            selected ? "bg-cyan/10 hover:bg-cyan/15" : "hover:bg-surface-hover",
        ].join(" "), children: [selectMode && (_jsx("div", { className: "flex items-center justify-center", children: _jsx("span", { "aria-hidden": true, className: [
                        "flex h-4 w-4 items-center justify-center rounded border font-mono text-[10px]",
                        selected ? "border-cyan bg-cyan/20 text-cyan" : "border-border text-transparent",
                    ].join(" "), children: "\u2713" }) })), _jsx("div", { className: "font-mono text-xs text-text-muted", children: index }), _jsx("div", { className: `font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`, children: formatLapTime(lap.lapTime) }), _jsx("div", { className: "truncate font-sans text-[13px] text-text", children: session?.track ?? _jsx("span", { className: "text-text-dim", children: "unknown" }) }), _jsx("div", { className: "truncate font-sans text-[12px] text-text-muted", children: session?.car ?? _jsx("span", { className: "text-text-dim", children: "unknown" }) }), _jsx("div", { className: "font-mono text-[11px] uppercase tracking-[0.05em] text-text-muted", children: session?.simulator ?? "—" }), _jsx("div", { className: "font-mono text-xs text-text-muted", children: formatTime(lap.recordedAt) }), _jsxs("div", { className: "font-mono text-[11px]", children: [lap.personalBest && _jsx("span", { className: "text-ok", children: "PB" }), !lap.valid && _jsx("span", { className: "text-accent", children: "INVAL" })] })] }));
}
function Toggle({ label, value, onChange, }) {
    return (_jsxs("button", { onClick: () => onChange(!value), className: [
            "self-end rounded border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors",
            value
                ? "border-cyan/30 bg-cyan/10 text-cyan"
                : "border-border text-text-muted hover:bg-surface-hover",
        ].join(" "), children: [value ? "✓" : "○", " ", label] }));
}
