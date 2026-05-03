import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useNavigate } from "react-router-dom";
import { Badge } from "./ui/Badge";
import { formatDate, formatDuration, formatTime } from "../lib/format";
/**
 * Single row in a sessions table. Navigation prefers the HATEOAS `self` link
 * provided by the backend; we treat it as opaque, only extracting the trailing
 * UID segment so we can hand it to React Router. This means the backend can
 * change its path scheme and the frontend follows.
 */
export function SessionRow({ session }) {
    const navigate = useNavigate();
    const selfUrl = session._links.self ?? "";
    const uidFromLink = selfUrl ? selfUrl.split("/").filter(Boolean).pop() : session.uid;
    return (_jsxs("button", { onClick: () => navigate(`/sessions/${uidFromLink}`), className: "grid w-full grid-cols-[90px_1fr_120px_90px_140px] items-center gap-3 rounded-md p-[10px] text-left transition-colors hover:bg-surface-hover", children: [_jsxs("div", { className: "font-mono text-[11px] text-text-muted", children: [formatDate(session.startedAt), _jsx("br", {}), _jsx("span", { className: "text-text-dim", children: formatTime(session.startedAt) })] }), _jsxs("div", { children: [_jsx("div", { className: "font-sans text-[13px] font-medium text-text", children: session.track ?? "Unknown" }), _jsx("div", { className: "font-sans text-[11px] text-text-muted", children: session.car ?? "Unknown" })] }), _jsx("div", { className: "flex items-center gap-2", children: _jsx(Badge, { type: session.sessionType }) }), _jsx("div", { className: "font-mono text-[11px] uppercase tracking-[0.05em] text-text-muted", children: session.simulator }), _jsx("div", { className: "font-mono text-[12px] text-text-muted", children: formatDuration(session.startedAt, session.endedAt) })] }));
}
