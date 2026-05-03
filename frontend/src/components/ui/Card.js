import { jsx as _jsx } from "react/jsx-runtime";
/** Reusable surface used for stat panels, table containers, etc. */
export function Card({ children, className = "", ...rest }) {
    return (_jsx("div", { ...rest, className: `rounded-lg border border-border bg-surface p-[22px] ${className}`, children: children }));
}
