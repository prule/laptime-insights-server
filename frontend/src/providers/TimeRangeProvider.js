import { jsx as _jsx } from "react/jsx-runtime";
/**
 * Global time range filter applied to dashboard / sessions / laps queries.
 *
 * The selector exposes five preset windows — `1m`, `3m`, `6m`, `1y`, `all` —
 * which are translated to an inclusive ISO-8601 `from` instant (and a `null`
 * upper bound, since "now" always means "up to the current moment"). Queries
 * pass the resulting `fromIso` to the backend as the `from` query param of
 * `/api/1/sessions` and `/api/1/laps`.
 *
 * The active selection is persisted to `localStorage` under the same pattern
 * as DataModeProvider so the user's choice survives reloads.
 */
import { createContext, useCallback, useContext, useMemo, useState, } from "react";
export const TIME_RANGE_OPTIONS = [
    { key: "1m", label: "1M", sub: "Last month" },
    { key: "3m", label: "3M", sub: "Last 3 months" },
    { key: "6m", label: "6M", sub: "Last 6 months" },
    { key: "1y", label: "1Y", sub: "Last year" },
    { key: "all", label: "All", sub: "All time" },
];
const STORAGE_KEY = "lti.timeRange";
const DEFAULT_RANGE = "1m";
const TimeRangeContext = createContext(null);
function readRange() {
    if (typeof window === "undefined")
        return DEFAULT_RANGE;
    const stored = window.localStorage.getItem(STORAGE_KEY);
    switch (stored) {
        case "1m":
        case "3m":
        case "6m":
        case "1y":
        case "all":
            return stored;
        default:
            return DEFAULT_RANGE;
    }
}
const ONE_DAY_MS = 86_400_000;
const APPROX_MONTH_MS = 30 * ONE_DAY_MS;
/** Compute the inclusive `from` instant for a given preset, anchored at `now`. */
export function computeFromIso(range, now = new Date()) {
    if (range === "all")
        return null;
    const ms = (() => {
        switch (range) {
            case "1m":
                return APPROX_MONTH_MS;
            case "3m":
                return 3 * APPROX_MONTH_MS;
            case "6m":
                return 6 * APPROX_MONTH_MS;
            case "1y":
                return 365 * ONE_DAY_MS;
        }
    })();
    return new Date(now.getTime() - ms).toISOString();
}
/**
 * Picks a bucket plan that keeps each chart readable: weekly buckets while
 * the range fits in a quarter, monthly thereafter. `all` defaults to a wide
 * monthly view; the chart layer is free to widen further if the underlying
 * data spans more than 24 months.
 */
export function computeBucketPlan(range) {
    switch (range) {
        case "1m":
            return { count: 4, unit: "week" };
        case "3m":
            return { count: 12, unit: "week" };
        case "6m":
            return { count: 6, unit: "month" };
        case "1y":
            return { count: 12, unit: "month" };
        case "all":
            return { count: 24, unit: "month" };
    }
}
export function TimeRangeProvider({ children }) {
    const [range, setRangeState] = useState(readRange);
    const setRange = useCallback((next) => {
        setRangeState(next);
        window.localStorage.setItem(STORAGE_KEY, next);
    }, []);
    // `fromIso` is recomputed when the selection changes. We deliberately don't
    // recompute on every render — anchoring on selection-change is "good enough"
    // for cache keys; if a session lasts long enough for the boundary to drift,
    // the user can re-pick the range or reload.
    const fromIso = useMemo(() => computeFromIso(range), [range]);
    const bucketPlan = useMemo(() => computeBucketPlan(range), [range]);
    const value = useMemo(() => ({ range, setRange, fromIso, bucketPlan }), [range, setRange, fromIso, bucketPlan]);
    return _jsx(TimeRangeContext.Provider, { value: value, children: children });
}
export function useTimeRange() {
    const ctx = useContext(TimeRangeContext);
    if (!ctx)
        throw new Error("useTimeRange must be used inside TimeRangeProvider");
    return ctx;
}
