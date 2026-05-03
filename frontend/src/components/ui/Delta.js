import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { formatLapTime } from "../../lib/format";
/**
 * Renders a signed difference between two lap times. Positive deltas are
 * shown in the accent color (slower), negative in green (faster).
 *
 * Pass `referenceMs = null` for the row that *is* the reference — renders an
 * em dash with no sign.
 */
export function Delta({ ms, referenceMs }) {
    if (referenceMs == null || ms === referenceMs) {
        return _jsx("span", { className: "font-mono text-xs text-text-dim", children: "\u2014" });
    }
    const diff = ms - referenceMs;
    const sign = diff > 0 ? "+" : "-";
    const colorClass = diff > 0 ? "text-accent" : "text-ok";
    return (_jsxs("span", { className: `font-mono text-xs font-semibold ${colorClass}`, children: [sign, formatLapTime(Math.abs(diff)).replace(/^0:/, "")] }));
}
