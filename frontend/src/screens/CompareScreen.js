import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { useMemo } from "react";
import { useLapComparison } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { GearMismatchStrip } from "../components/ui/GearMismatchStrip";
import { LapPicker } from "../components/LapPicker";
import { SectionHeader } from "../components/ui/SectionHeader";
import { SpeedDeltaTrace } from "../components/ui/SpeedDeltaTrace";
import { TelemetryTrace } from "../components/ui/TelemetryTrace";
import { formatLapTime } from "../lib/format";
import { getString, useUrlState } from "../hooks/useUrlState";
const COLOR_LAP1 = "#00d4ff";
const COLOR_LAP2 = "#e8212a";
/**
 * Lap-comparison screen.
 *
 * URL state owns `track`, `lap1`, `lap2`. Reload-safe and shareable.
 *
 * Each lap slot is a `LapPicker` — a button that pops a modal with the same
 * track/car/PB filters and pagination as the Laps screen. The optional
 * `track` URL param pre-fills the picker's track filter so jumps from
 * SessionDetail's "vs best" / "vs PB" buttons land on the right context.
 */
export function CompareScreen() {
    const [params, setParam, setMany] = useUrlState();
    const track = getString(params, "track");
    const lap1Uid = getString(params, "lap1");
    const lap2Uid = getString(params, "lap2");
    const comparisonQuery = useLapComparison(lap1Uid, lap2Uid);
    const series = useMemo(() => {
        if (!comparisonQuery.data)
            return [];
        return [
            { samples: comparisonQuery.data.lap1.samples, color: COLOR_LAP1, label: "Lap 1" },
            { samples: comparisonQuery.data.lap2.samples, color: COLOR_LAP2, label: "Lap 2" },
        ];
    }, [comparisonQuery.data]);
    return (_jsxs("div", { className: "h-full overflow-y-auto px-8 py-7", children: [_jsxs(Card, { className: "mb-4", children: [_jsx(SectionHeader, { title: "Pick laps to compare", sub: "Both pickers open a searchable list \u2014 filter by track/car, paginate, click to pick. URL stays in sync so the comparison is shareable." }), _jsxs("div", { className: "flex flex-wrap items-end gap-3", children: [_jsx(LapPicker, { label: "Lap 1", accentColor: COLOR_LAP1, defaultTrack: track, selectedUid: lap1Uid, disabledLapUid: lap2Uid, onSelect: (v) => setParam("lap1", v) }), _jsx(LapPicker, { label: "Lap 2", accentColor: COLOR_LAP2, defaultTrack: track, selectedUid: lap2Uid, disabledLapUid: lap1Uid, onSelect: (v) => setParam("lap2", v) }), (lap1Uid || lap2Uid || track) && (_jsx("button", { onClick: () => setMany({ track: undefined, lap1: undefined, lap2: undefined }), className: "self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text", children: "Reset" }))] })] }), !lap1Uid || !lap2Uid ? (_jsx(Card, { children: _jsx(EmptyState, { title: "Pick two laps", description: "Use the pickers above. Tip: from a session detail page, the per-row 'vs best' or 'vs PB' buttons land here with both laps preselected." }) })) : comparisonQuery.isLoading ? (_jsx(Card, { children: _jsx(LoadingState, {}) })) : comparisonQuery.isError ? (_jsx(Card, { children: _jsx(ErrorState, { error: comparisonQuery.error, onRetry: () => comparisonQuery.refetch() }) })) : comparisonQuery.data ? (_jsxs(_Fragment, { children: [_jsx(Card, { className: "mb-4", children: _jsxs("div", { className: "grid grid-cols-2 gap-6", children: [_jsx(LapHeader, { color: COLOR_LAP1, label: "Lap 1", lapNumber: comparisonQuery.data.lap1.lapNumber, lapTimeMs: comparisonQuery.data.lap1.lapTimeMs, personalBest: comparisonQuery.data.lap1.personalBest }), _jsx(LapHeader, { color: COLOR_LAP2, label: "Lap 2", lapNumber: comparisonQuery.data.lap2.lapNumber, lapTimeMs: comparisonQuery.data.lap2.lapTimeMs, personalBest: comparisonQuery.data.lap2.personalBest })] }) }), _jsxs(Card, { className: "mb-4", children: [_jsx(SectionHeader, { title: "Speed (KPH)", sub: "Both laps overlaid against splinePosition (0 \u2192 1)" }), _jsx(TelemetryTrace, { series: series, field: "speedKph", height: 180, unit: "kph" })] }), _jsxs(Card, { className: "mb-4", children: [_jsx(SectionHeader, { title: "Speed delta", sub: "Lap 1 minus Lap 2 at every 1% of track length" }), _jsx(SpeedDeltaTrace, { lap1: comparisonQuery.data.lap1.samples, lap2: comparisonQuery.data.lap2.samples })] }), _jsxs(Card, { className: "mb-4", children: [_jsx(SectionHeader, { title: "Gear mismatch", sub: "Red strips mark sectors where the two laps used different gears" }), _jsx(GearMismatchStrip, { lap1: comparisonQuery.data.lap1.samples, lap2: comparisonQuery.data.lap2.samples })] }), _jsxs("div", { className: "grid grid-cols-2 gap-4", children: [_jsxs(Card, { children: [_jsx(SectionHeader, { title: "Throttle" }), _jsx(TelemetryTrace, { series: series, field: "throttle", height: 120, yMin: 0, yMax: 1 })] }), _jsxs(Card, { children: [_jsx(SectionHeader, { title: "Brake" }), _jsx(TelemetryTrace, { series: series, field: "brake", height: 120, yMin: 0, yMax: 1 })] })] })] })) : null] }));
}
function LapHeader({ color, label, lapNumber, lapTimeMs, personalBest, }) {
    return (_jsxs("div", { className: "flex items-center gap-4", children: [_jsx("div", { className: "h-3 w-3 rounded-full", style: { background: color } }), _jsxs("div", { children: [_jsxs("div", { className: "font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted", children: [label, " ", personalBest && _jsx("span", { className: "text-ok", children: "\u00B7 PB" })] }), _jsx("div", { className: "font-mono text-xl text-text", children: formatLapTime(lapTimeMs) }), _jsxs("div", { className: "font-sans text-xs text-text-muted", children: ["Lap #", lapNumber] })] })] }));
}
