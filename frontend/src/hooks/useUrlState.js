import { useCallback, useMemo } from "react";
import { useSearchParams } from "react-router-dom";
/**
 * Tiny wrapper over `useSearchParams` for filter/paging state.
 *
 * The URL is the single source of truth — reload, deep-link, or share-paste a
 * URL and the screen restores its filters. Empty / undefined values are
 * stripped so the URL stays tidy (`?page=1&track=Monza`, never
 * `?page=&track=Monza&car=`).
 *
 * Usage:
 * ```ts
 * const [params, setParam, setMany] = useUrlState();
 * params.get("track");                       // "Monza" | null
 * setParam("track", undefined);              // remove `track`
 * setMany({ track: "Spa", page: "1" });      // batch update
 * ```
 */
export function useUrlState() {
    const [searchParams, setSearchParams] = useSearchParams();
    const setParam = useCallback((key, value) => {
        setSearchParams((prev) => {
            const next = new URLSearchParams(prev);
            if (value === undefined || value === null || value === "" || value === false) {
                next.delete(key);
            }
            else {
                next.set(key, String(value));
            }
            return next;
        }, { replace: true });
    }, [setSearchParams]);
    const setMany = useCallback((updates) => {
        setSearchParams((prev) => {
            const next = new URLSearchParams(prev);
            for (const [k, v] of Object.entries(updates)) {
                if (v === undefined || v === null || v === "" || v === false) {
                    next.delete(k);
                }
                else {
                    next.set(k, String(v));
                }
            }
            return next;
        }, { replace: true });
    }, [setSearchParams]);
    return useMemo(() => [searchParams, setParam, setMany], [searchParams, setParam, setMany]);
}
/** Read a string param. Returns `undefined` for missing or empty. */
export function getString(params, key) {
    const v = params.get(key);
    return v && v.length > 0 ? v : undefined;
}
/** Read a 1-based positive integer param with a default. */
export function getInt(params, key, fallback) {
    const v = params.get(key);
    if (!v)
        return fallback;
    const parsed = Number.parseInt(v, 10);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : fallback;
}
/** Read a boolean param. `"true"` / `"1"` / present-but-empty count as true. */
export function getBool(params, key, fallback = false) {
    const v = params.get(key);
    if (v === null)
        return fallback;
    if (v === "" || v === "true" || v === "1")
        return true;
    if (v === "false" || v === "0")
        return false;
    return fallback;
}
