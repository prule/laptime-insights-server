import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useState } from "react";
import { useLap, useSession } from "../api/queries";
import { LapBrowser } from "./LapBrowser";
import { Modal } from "./ui/Modal";
import { formatDate, formatLapTime } from "../lib/format";
/**
 * Button + modal lap picker. Replaces the old `<select>` dropdown — the
 * browser inside the modal supports the same filters/pagination as the Laps
 * screen, so this scales to thousands of laps.
 *
 * The button itself shows the current selection's lap time + track + date so
 * users can confirm at a glance without re-opening the modal.
 */
export function LapPicker({ label, defaultTrack, selectedUid, onSelect, disabledLapUid, accentColor, trackFilter, }) {
    const [open, setOpen] = useState(false);
    const lapQuery = useLap(selectedUid);
    const sessionQuery = useSession(lapQuery.data?.sessionUid);
    const lap = lapQuery.data;
    const session = sessionQuery.data;
    const summary = lap
        ? `${formatLapTime(lap.lapTime)} · ${session?.track ?? "?"} · ${formatDate(session?.startedAt)}`
        : "— pick a lap —";
    return (_jsxs("div", { className: "flex flex-col gap-1", children: [_jsx("span", { className: "font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted", children: label }), _jsxs("div", { className: "flex items-stretch gap-1", children: [_jsx("button", { onClick: () => setOpen(true), className: "min-w-[260px] flex-1 rounded border border-border bg-surface px-3 py-2 text-left font-sans text-[13px] text-text hover:border-cyan/40", children: _jsxs("div", { className: "flex items-center gap-2", children: [accentColor && (_jsx("span", { className: "h-2 w-2 flex-shrink-0 rounded-full", style: { background: accentColor } })), _jsx("span", { className: lap ? "" : "text-text-muted", children: summary })] }) }), selectedUid && (_jsx("button", { onClick: () => onSelect(undefined), title: "Clear", className: "rounded border border-border px-2 font-mono text-[12px] text-text-muted hover:text-text", children: "\u00D7" }))] }), _jsx(Modal, { open: open, onClose: () => setOpen(false), title: label, children: _jsx(LapBrowser, { defaultTrack: trackFilter || defaultTrack, disabledLapUid: disabledLapUid, onPick: (uid) => {
                        onSelect(uid);
                        setOpen(false);
                    } }) })] }));
}
