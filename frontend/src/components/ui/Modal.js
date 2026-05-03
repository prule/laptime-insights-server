import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useEffect } from "react";
/**
 * Lightweight modal overlay. Backdrop click + Escape close it. No focus
 * trapping — we keep this simple and only use modals for short-lived,
 * pick-and-go interactions.
 */
export function Modal({ open, onClose, title, children, maxWidthClass = "max-w-3xl" }) {
    useEffect(() => {
        if (!open)
            return;
        const onKey = (e) => {
            if (e.key === "Escape")
                onClose();
        };
        document.addEventListener("keydown", onKey);
        document.body.style.overflow = "hidden";
        return () => {
            document.removeEventListener("keydown", onKey);
            document.body.style.overflow = "";
        };
    }, [open, onClose]);
    if (!open)
        return null;
    return (_jsx("div", { role: "dialog", "aria-modal": true, className: "fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-6", onClick: onClose, children: _jsxs("div", { className: `flex max-h-[85vh] w-full flex-col overflow-hidden rounded-lg border border-border bg-surface shadow-2xl ${maxWidthClass}`, onClick: (e) => e.stopPropagation(), children: [title && (_jsxs("div", { className: "flex items-center justify-between border-b border-border px-5 py-3", children: [_jsx("div", { className: "font-sans text-sm font-medium text-text", children: title }), _jsx("button", { onClick: onClose, "aria-label": "Close", className: "font-mono text-[16px] leading-none text-text-muted hover:text-text", children: "\u00D7" })] })), _jsx("div", { className: "flex-1 overflow-auto p-5", children: children })] }) }));
}
