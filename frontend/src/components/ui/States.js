import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function LoadingState({ label = "Loading…" }) {
    return (_jsx("div", { className: "flex h-32 items-center justify-center font-mono text-xs uppercase tracking-[0.08em] text-text-muted", children: label }));
}
export function ErrorState({ error, onRetry }) {
    const message = error instanceof Error ? error.message : String(error);
    return (_jsxs("div", { className: "flex h-32 flex-col items-center justify-center gap-2 font-sans text-sm text-accent", children: [_jsx("div", { children: "Failed to load data" }), _jsx("div", { className: "font-mono text-[11px] text-text-muted", children: message }), onRetry && (_jsx("button", { onClick: onRetry, className: "rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text hover:bg-surface-hover", children: "Retry" }))] }));
}
export function EmptyState({ title, description }) {
    return (_jsxs("div", { className: "flex h-32 flex-col items-center justify-center gap-1 text-center font-sans text-sm text-text-muted", children: [_jsx("div", { className: "text-text", children: title }), description && _jsx("div", { className: "text-xs", children: description })] }));
}
