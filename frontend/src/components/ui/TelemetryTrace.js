import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
/**
 * Continuous trace plotted against splinePosition (0 → 1).
 *
 * Multiple series share the same X axis and Y range so two laps overlay 1:1
 * spatially. Each series renders its own polyline; the parent decides the
 * draw order (typically reference lap drawn first, focus lap last).
 */
export function TelemetryTrace({ series, field, height = 140, unit, yMin, yMax, }) {
    const all = series.flatMap((s) => s.samples.map((sample) => sample[field]));
    if (all.length === 0)
        return null;
    const min = yMin ?? Math.min(...all);
    const max = yMax ?? Math.max(...all);
    const span = max - min || 1;
    const width = 1000; // viewBox width — scales via CSS
    const pathFor = (samples) => samples
        .map((sample, i) => {
        const x = sample.splinePosition * width;
        const y = height - ((sample[field] - min) / span) * height;
        return `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
    })
        .join(" ");
    return (_jsxs("div", { className: "relative", children: [_jsxs("svg", { viewBox: `0 0 ${width} ${height}`, preserveAspectRatio: "none", className: "block w-full", style: { height }, children: [[0, 0.25, 0.5, 0.75, 1].map((t) => {
                        const y = height * t;
                        return (_jsx("line", { x1: 0, x2: width, y1: y, y2: y, stroke: "rgba(255,255,255,0.04)", strokeWidth: 1 }, t));
                    }), series.map((s, idx) => (_jsx("path", { d: pathFor(s.samples), fill: "none", stroke: s.color, strokeWidth: 1.5, vectorEffect: "non-scaling-stroke", opacity: idx === series.length - 1 ? 1 : 0.7 }, `${s.label}-${idx}`)))] }), _jsxs("div", { className: "mt-1 flex justify-between font-mono text-[10px] text-text-dim", children: [_jsxs("span", { children: [min.toFixed(field === "speedKph" ? 0 : 2), unit ? ` ${unit}` : ""] }), _jsxs("span", { children: [max.toFixed(field === "speedKph" ? 0 : 2), unit ? ` ${unit}` : ""] })] })] }));
}
