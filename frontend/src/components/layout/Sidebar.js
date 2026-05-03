import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { NavLink } from "react-router-dom";
import { NAV_ITEMS } from "../../config/navigation";
import { useDataMode } from "../../providers/DataModeProvider";
export function Sidebar() {
    const { mode, apiUrl, setMode } = useDataMode();
    const isLive = mode === "live";
    return (_jsxs("aside", { className: "flex h-full w-[220px] flex-shrink-0 flex-col border-r border-border bg-sidebar", children: [_jsxs("div", { className: "flex items-center gap-[10px] border-b border-border px-5 py-[22px]", children: [_jsx("div", { className: "flex h-[30px] w-[30px] items-center justify-center rounded-[7px] bg-gradient-to-br from-accent to-orange font-mono text-[13px] font-bold text-white", children: "L" }), _jsxs("div", { children: [_jsx("div", { className: "font-mono text-[13px] font-medium tracking-tight text-text", children: "LapTime" }), _jsx("div", { className: "font-mono text-[10px] tracking-[0.05em] text-text-muted", children: "INSIGHTS" })] })] }), _jsx("nav", { className: "flex-1 py-3", children: NAV_ITEMS.map((item) => (_jsx(NavLink, { to: item.path, end: item.path === "/", className: ({ isActive }) => [
                        "mb-[2px] flex items-center gap-[10px] rounded-r-md border-l-2 px-[18px] py-[10px] text-left transition-colors",
                        isActive
                            ? "border-accent bg-white/[0.06] text-text"
                            : "border-transparent text-text-muted hover:bg-white/[0.03]",
                    ].join(" "), children: ({ isActive }) => (_jsxs(_Fragment, { children: [_jsx("span", { className: `text-base ${isActive ? "text-accent" : "text-text-muted"}`, children: item.icon }), _jsx("span", { className: "font-sans text-[13px] font-medium", children: item.label })] })) }, item.id))) }), _jsx("div", { className: "border-t border-border p-4", children: _jsxs("button", { onClick: () => setMode(isLive ? "mock" : "live"), className: [
                        "flex w-full items-center gap-2 rounded-md border px-3 py-[9px] text-left transition-colors",
                        isLive
                            ? "border-ok/25 bg-ok/10"
                            : "border-border bg-white/[0.04] hover:bg-white/[0.06]",
                    ].join(" "), title: isLive ? "Switch to mock data" : "Switch to live data", children: [_jsx("span", { className: [
                                "h-[7px] w-[7px] flex-shrink-0 rounded-full",
                                isLive ? "bg-ok shadow-[0_0_6px_#22c55e]" : "bg-text-muted",
                            ].join(" ") }), _jsxs("div", { className: "text-left", children: [_jsx("div", { className: `font-mono text-[10px] tracking-[0.08em] ${isLive ? "text-ok" : "text-text-muted"}`, children: isLive ? "LIVE" : "MOCK" }), isLive && (_jsx("div", { className: "truncate font-mono text-[9px] text-text-muted/70", children: apiUrl || "(relative URL via dev proxy)" }))] })] }) })] }));
}
