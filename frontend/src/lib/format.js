/**
 * Formatting helpers used across screens. Keep pure (no React) so they're
 * easy to unit-test and to reuse in non-component code.
 */
export function formatLapTime(ms) {
    if (ms == null || !Number.isFinite(ms))
        return "—";
    const totalMs = Math.max(0, Math.round(ms));
    const minutes = Math.floor(totalMs / 60_000);
    const seconds = Math.floor((totalMs % 60_000) / 1000);
    const millis = totalMs % 1000;
    return `${minutes}:${String(seconds).padStart(2, "0")}.${String(millis).padStart(3, "0")}`;
}
export function formatDate(iso) {
    if (!iso)
        return "—";
    return new Date(iso).toLocaleDateString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}
export function formatShortDate(iso) {
    if (!iso)
        return "—";
    return new Date(iso).toLocaleDateString("en-GB", { day: "2-digit", month: "short" });
}
export function formatTime(iso) {
    if (!iso)
        return "—";
    return new Date(iso).toLocaleTimeString("en-GB", { hour: "2-digit", minute: "2-digit" });
}
export function formatDuration(startIso, endIso) {
    if (!startIso || !endIso)
        return "—";
    const ms = new Date(endIso).getTime() - new Date(startIso).getTime();
    if (ms < 0)
        return "—";
    const totalSec = Math.floor(ms / 1000);
    const hours = Math.floor(totalSec / 3600);
    const minutes = Math.floor((totalSec % 3600) / 60);
    const seconds = totalSec % 60;
    return hours > 0
        ? `${hours}:${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`
        : `${minutes}:${String(seconds).padStart(2, "0")}`;
}
export function formatNumber(value, decimals = 0) {
    return Number(value).toLocaleString("en", {
        minimumFractionDigits: decimals,
        maximumFractionDigits: decimals,
    });
}
