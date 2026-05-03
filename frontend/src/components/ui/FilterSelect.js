import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
export function FilterSelect({ label, value, options, onChange }) {
    return (_jsxs("label", { className: "flex flex-col gap-1", children: [_jsx("span", { className: "font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted", children: label }), _jsxs("select", { value: value ?? "", onChange: (e) => onChange(e.target.value || undefined), className: "rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none focus:border-cyan/40", children: [_jsx("option", { value: "", children: "All" }), options.map((opt) => (_jsx("option", { value: opt, children: opt }, opt)))] })] }));
}
