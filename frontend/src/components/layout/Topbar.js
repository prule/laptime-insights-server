import { jsx as _jsx } from "react/jsx-runtime";
import { useLocation, useMatch } from "react-router-dom";
import { useSession } from "../../api/queries";
import { formatDate } from "../../lib/format";
const SCREEN_LABELS = {
    "/": "Overview",
    "/sessions": "Sessions",
    "/laps": "Lap Search",
    "/compare": "Lap Comparison",
};
export function Topbar() {
    const location = useLocation();
    const sessionMatch = useMatch("/sessions/:uid");
    const sessionUid = sessionMatch?.params.uid;
    const { data: session } = useSession(sessionUid);
    const label = sessionUid
        ? session
            ? `${session.track ?? "Unknown track"} · ${formatDate(session.startedAt)}`
            : "Session"
        : SCREEN_LABELS[location.pathname] ?? "LapTime Insights";
    return (_jsx("header", { className: "flex h-[52px] flex-shrink-0 items-center gap-4 border-b border-border bg-bg px-7", children: _jsx("div", { className: "font-sans text-[15px] font-medium text-text", children: label }) }));
}
