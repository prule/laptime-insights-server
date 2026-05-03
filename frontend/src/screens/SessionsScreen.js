import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { useSessionOptions, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, EmptyState, LoadingState } from "../components/ui/States";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import { SessionRow } from "../components/SessionRow";
import { getString, useUrlState } from "../hooks/useUrlState";
import { useTimeRange } from "../providers/TimeRangeProvider";
/**
 * Filters live in the URL querystring. Reload-safe and shareable: a link
 * like `/sessions?track=Monza&car=Ferrari%20488%20GT3` restores the same
 * filter state.
 */
export function SessionsScreen() {
    const optionsQuery = useSessionOptions();
    const [params, setParam, setMany] = useUrlState();
    const filters = {
        track: getString(params, "track"),
        car: getString(params, "car"),
        simulator: getString(params, "simulator"),
    };
    const facetsActive = !!(filters.track || filters.car || filters.simulator);
    const { fromIso } = useTimeRange();
    const sessionsQuery = useSessions({
        ...filters,
        from: fromIso ?? undefined,
        size: 50,
        sort: "startedAt:DESC",
    });
    return (_jsxs("div", { className: "h-full overflow-y-auto px-8 py-7", children: [_jsxs(Card, { className: "mb-4", children: [_jsx(SectionHeader, { title: "Filter", sub: "Filters are written to the URL \u2014 reload or share the link to restore state" }), _jsxs("div", { className: "flex flex-wrap gap-3", children: [_jsx(FilterSelect, { label: "Track", value: filters.track, options: optionsQuery.data?.tracks ?? [], onChange: (v) => setParam("track", v) }), _jsx(FilterSelect, { label: "Car", value: filters.car, options: optionsQuery.data?.cars ?? [], onChange: (v) => setParam("car", v) }), _jsx(FilterSelect, { label: "Simulator", value: filters.simulator, options: optionsQuery.data?.simulators ?? [], onChange: (v) => setParam("simulator", v) }), facetsActive && (_jsx("button", { onClick: () => setMany({ track: undefined, car: undefined, simulator: undefined }), className: "self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text", children: "Clear" }))] })] }), _jsxs(Card, { children: [_jsx(SectionHeader, { title: "Sessions", sub: sessionsQuery.data
                            ? `${sessionsQuery.data.total} match · showing ${sessionsQuery.data.items.length}`
                            : undefined }), sessionsQuery.isLoading && _jsx(LoadingState, {}), sessionsQuery.isError && (_jsx(ErrorState, { error: sessionsQuery.error, onRetry: () => sessionsQuery.refetch() })), sessionsQuery.data && sessionsQuery.data.items.length === 0 && (_jsx(EmptyState, { title: "No sessions match", description: "Try clearing filters or seeding the database." })), sessionsQuery.data && sessionsQuery.data.items.length > 0 && (_jsx("div", { className: "flex flex-col gap-1", children: sessionsQuery.data.items.map((s) => (_jsx(SessionRow, { session: s }, s.uid))) }))] })] }));
}
