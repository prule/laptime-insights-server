import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function SectionHeader({ title, sub, action, onAction }) {
    return (_jsxs("div", { className: "mb-4 flex items-baseline justify-between", children: [_jsxs("div", { children: [_jsx("div", { className: "font-sans text-sm font-medium text-text", children: title }), sub && _jsx("div", { className: "font-sans text-xs text-text-muted", children: sub })] }), action && (_jsx("button", { onClick: onAction, className: "font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted transition-colors hover:text-cyan", children: action }))] }));
}
