/**
 * TanStack Query hooks. Each hook depends on the active data mode + apiUrl
 * (from the DataModeProvider) so flipping modes invalidates the cache.
 *
 * Endpoints + filters mirror the Ktor controllers:
 *   - GET /api/1/sessions          (SearchSessionController)
 *   - GET /api/1/sessions/{uid}    (FindSessionController)
 *   - GET /api/1/sessions/options  (SearchOptionsController)
 *   - GET /api/1/laps              (SearchLapController, filter by sessionUid)
 *   - GET /api/1/laps/{uid}        (FindLapController)
 */
import { useQuery } from "@tanstack/react-query";
import { apiGet, buildQuery } from "./client";
import { useDataMode } from "../providers/DataModeProvider";
function useApiContext() {
    const { mode, apiUrl } = useDataMode();
    return { mode, apiBase: apiUrl };
}
const KEYS = {
    options: (ctx) => ["session-options", ctx.mode, ctx.apiBase],
    sessions: (ctx, filters) => ["sessions", ctx.mode, ctx.apiBase, filters],
    session: (ctx, uid) => ["session", ctx.mode, ctx.apiBase, uid],
    laps: (ctx, params) => ["laps", ctx.mode, ctx.apiBase, params],
    lapTelemetry: (ctx, lapUid) => ["lap-telemetry", ctx.mode, ctx.apiBase, lapUid],
    compare: (ctx, lap1Uid, lap2Uid) => ["compare-laps", ctx.mode, ctx.apiBase, lap1Uid, lap2Uid],
};
export function useSessionOptions() {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.options(ctx),
        queryFn: () => apiGet(ctx, "/api/1/sessions/options"),
    });
}
export function useSessions(filters = {}) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.sessions(ctx, filters),
        queryFn: () => apiGet(ctx, `/api/1/sessions${buildQuery({
            ...filters,
            page: filters.page ?? 1,
            size: filters.size ?? 25,
            sort: filters.sort ?? "startedAt:DESC",
        })}`),
        placeholderData: (previous) => previous,
    });
}
export function useLap(uid) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: ["lap", ctx.mode, ctx.apiBase, uid],
        queryFn: () => apiGet(ctx, `/api/1/laps/${uid}`),
        enabled: !!uid,
        staleTime: 60_000,
    });
}
export function useSession(uid) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.session(ctx, uid),
        queryFn: () => apiGet(ctx, `/api/1/sessions/${uid}`),
        enabled: !!uid,
    });
}
export function useSessionLaps(sessionUid, paging = {}) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.laps(ctx, { sessionUid, ...paging }),
        queryFn: () => apiGet(ctx, `/api/1/laps${buildQuery({
            sessionUid,
            page: paging.page ?? 1,
            size: paging.size ?? 200,
            sort: paging.sort ?? "lapNumber:ASC",
        })}`),
        enabled: !!sessionUid,
    });
}
export function useLapTelemetry(lapUid) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.lapTelemetry(ctx, lapUid),
        queryFn: () => apiGet(ctx, `/api/1/laps/${lapUid}/telemetry`),
        enabled: !!lapUid,
        // Telemetry is immutable for a given lap — keep it cached longer.
        staleTime: 5 * 60_000,
    });
}
export function useLapComparison(lap1Uid, lap2Uid) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.compare(ctx, lap1Uid, lap2Uid),
        queryFn: () => apiGet(ctx, `/api/1/laps/compare?lap1Uid=${lap1Uid}&lap2Uid=${lap2Uid}`),
        enabled: !!lap1Uid && !!lap2Uid,
        staleTime: 5 * 60_000,
    });
}
/**
 * Returns the fastest valid lap of the supplied session, or `undefined` while
 * loading / when the session has no valid laps yet. Cheap — `size=1` with a
 * server-side sort.
 */
export function useSessionBestLap(sessionUid) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: ["session-best-lap", ctx.mode, ctx.apiBase, sessionUid],
        queryFn: async () => {
            const page = await apiGet(ctx, `/api/1/laps${buildQuery({
                sessionUid,
                validLap: true,
                page: 1,
                size: 1,
                sort: "lapTime:ASC",
            })}`);
            return page.items[0] ?? null;
        },
        enabled: !!sessionUid,
        staleTime: 60_000,
    });
}
/**
 * Returns the fastest valid lap recorded at the supplied track across every
 * session — i.e. the all-time PB at that track. Backend join handles the
 * `track` filter.
 */
export function useTrackBestLap(track) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: ["track-best-lap", ctx.mode, ctx.apiBase, track],
        queryFn: async () => {
            const page = await apiGet(ctx, `/api/1/laps${buildQuery({
                track: track ?? undefined,
                validLap: true,
                page: 1,
                size: 1,
                sort: "lapTime:ASC",
            })}`);
            return page.items[0] ?? null;
        },
        enabled: !!track,
        staleTime: 60_000,
    });
}
export function useLaps(params = {}) {
    const ctx = useApiContext();
    return useQuery({
        queryKey: KEYS.laps(ctx, params),
        queryFn: () => apiGet(ctx, `/api/1/laps${buildQuery({
            validLap: params.validLap,
            personalBest: params.personalBest,
            car: params.car,
            track: params.track,
            simulator: params.simulator,
            page: params.page ?? 1,
            size: params.size ?? 50,
            sort: params.sort ?? "lapTime:ASC",
        })}`),
        placeholderData: (previous) => previous,
    });
}
