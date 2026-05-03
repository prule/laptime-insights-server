import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * Plots `speedKph(lap1) - speedKph(lap2)` resampled at every 1% of track
 * length, per the lap-comparison spec ("Speed Delta: Calculate the difference
 * in KPH at every 1% of the track length"). Positive delta = lap1 faster than
 * lap2 → renders in cyan; negative = lap2 faster → renders in accent (red).
 */
export function SpeedDeltaTrace({ lap1, lap2, height = 100 }) {
    if (lap1.length === 0 || lap2.length === 0)
        return null;
    const buckets = 100;
    const series1 = resample(lap1, buckets);
    const series2 = resample(lap2, buckets);
    const deltas = series1.map((v, i) => v - (series2[i] ?? 0));
    const absMax = Math.max(...deltas.map((d) => Math.abs(d)), 1);
    const width = 1000;
    const zeroY = height / 2;
    const path = deltas
        .map((d, i) => {
        const x = (i / (buckets - 1)) * width;
        const y = zeroY - (d / absMax) * (height / 2 - 4);
        return `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
    })
        .join(" ");
    return (_jsxs("div", { children: [_jsxs("svg", { viewBox: `0 0 ${width} ${height}`, preserveAspectRatio: "none", className: "block w-full", style: { height }, children: [_jsx("line", { x1: 0, x2: width, y1: zeroY, y2: zeroY, stroke: "rgba(255,255,255,0.12)", strokeWidth: 1 }), _jsx("path", { d: path, fill: "none", stroke: "#00d4ff", strokeWidth: 1.5, vectorEffect: "non-scaling-stroke" })] }), _jsxs("div", { className: "mt-1 flex justify-between font-mono text-[10px] text-text-dim", children: [_jsxs("span", { className: "text-cyan", children: ["+", absMax.toFixed(0), " kph (lap 1 faster)"] }), _jsxs("span", { className: "text-accent", children: ["-", absMax.toFixed(0), " kph (lap 2 faster)"] })] })] }));
}
/** Resample `speedKph` to `buckets` evenly-spaced splinePositions via linear interpolation. */
function resample(samples, buckets) {
    if (samples.length === 0)
        return [];
    const sorted = samples.slice().sort((a, b) => a.splinePosition - b.splinePosition);
    const out = new Array(buckets);
    let cursor = 0;
    for (let i = 0; i < buckets; i++) {
        const target = i / (buckets - 1);
        while (cursor < sorted.length - 1 &&
            (sorted[cursor + 1]?.splinePosition ?? 1) < target) {
            cursor++;
        }
        const a = sorted[cursor];
        const b = sorted[Math.min(cursor + 1, sorted.length - 1)];
        const span = b.splinePosition - a.splinePosition || 1;
        const ratio = (target - a.splinePosition) / span;
        out[i] = a.speedKph + (b.speedKph - a.speedKph) * Math.max(0, Math.min(1, ratio));
    }
    return out;
}
