import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
const ACCENT_BORDER = {
    accent: "from-accent",
    cyan: "from-cyan",
    ok: "from-ok",
    warn: "from-warn",
    orange: "from-orange",
    muted: "from-text-muted",
};
export function StatCard({ label, value, sub, accent = "cyan", small }) {
    return (_jsxs("div", { className: [
            "relative overflow-hidden rounded-lg border border-border bg-surface",
            small ? "px-[18px] py-4" : "px-[22px] py-5",
            "flex flex-col gap-1",
        ].join(" "), children: [_jsx("div", { className: `absolute inset-x-0 top-0 h-[2px] bg-gradient-to-r ${ACCENT_BORDER[accent]} to-transparent opacity-70` }), _jsx("div", { className: "font-sans text-[11px] font-medium uppercase tracking-[0.08em] text-text-muted", children: label }), _jsx("div", { className: [
                    "font-mono font-bold leading-tight tracking-tight text-text",
                    small ? "text-[22px]" : "text-[28px]",
                ].join(" "), children: value }), sub && _jsx("div", { className: "font-sans text-xs text-text-muted", children: sub })] }));
}
