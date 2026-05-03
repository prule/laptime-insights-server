import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
const FILL = {
    cyan: "from-cyan/80 to-cyan/30",
    accent: "from-accent/80 to-accent/30",
    warn: "from-warn/80 to-warn/30",
    ok: "from-ok/80 to-ok/30",
};
export function BarChart({ data, colorClass = "cyan", height = 80 }) {
    if (data.length === 0)
        return null;
    const max = Math.max(...data.map((d) => d.value), 1);
    return (_jsx("div", { className: "flex items-end gap-[6px]", style: { height }, children: data.map((d, i) => (_jsxs("div", { className: "flex h-full flex-1 flex-col items-center gap-1", children: [_jsx("div", { className: "flex w-full flex-1 items-end", children: _jsx("div", { className: `w-full rounded-t-[3px] bg-gradient-to-b ${FILL[colorClass]} transition-[height] duration-500`, style: { height: `${Math.max(4, (d.value / max) * 100)}%` } }) }), _jsx("div", { className: "font-mono text-[9px] text-text-dim whitespace-nowrap", children: d.label })] }, `${d.label}-${i}`))) }));
}
