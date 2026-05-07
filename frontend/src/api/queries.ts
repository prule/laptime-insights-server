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
import { apiGet, buildQuery, type ApiContext } from "./client";
import type {
  LapComparisonResource,
  LapResource,
  LapTelemetryResource,
  Page,
  PagingAndSort,
  SessionFilters,
  SessionOptionsResource,
  SessionResource,
} from "./types";
import { useDataMode } from "../providers/DataModeProvider";

function useApiContext(): ApiContext {
  const { mode, apiUrl } = useDataMode();
  return { mode, apiBase: apiUrl };
}

const KEYS = {
  options: (ctx: ApiContext) => ["session-options", ctx.mode, ctx.apiBase] as const,
  sessions: (ctx: ApiContext, filters: SessionFilters & PagingAndSort) =>
    ["sessions", ctx.mode, ctx.apiBase, filters] as const,
  session: (ctx: ApiContext, uid: string | undefined) =>
    ["session", ctx.mode, ctx.apiBase, uid] as const,
  laps: (ctx: ApiContext, params: { sessionUid?: string } & PagingAndSort) =>
    ["laps", ctx.mode, ctx.apiBase, params] as const,
  lapTelemetry: (ctx: ApiContext, lapUid: string | undefined) =>
    ["lap-telemetry", ctx.mode, ctx.apiBase, lapUid] as const,
  compare: (ctx: ApiContext, lap1Uid: string | undefined, lap2Uid: string | undefined) =>
    ["compare-laps", ctx.mode, ctx.apiBase, lap1Uid, lap2Uid] as const,
};

export function useSessionOptions() {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.options(ctx),
    queryFn: () => apiGet<SessionOptionsResource>(ctx, "/api/1/sessions/options"),
  });
}

export function useSessions(filters: SessionFilters & PagingAndSort = {}) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.sessions(ctx, filters),
    queryFn: () =>
      apiGet<Page<SessionResource>>(
        ctx,
        `/api/1/sessions${buildQuery({
          ...filters,
          page: filters.page ?? 1,
          size: filters.size ?? 25,
          sort: filters.sort ?? "startedAt:DESC",
        })}`,
      ),
    placeholderData: (previous) => previous,
  });
}

export function useLap(uid: string | undefined) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: ["lap", ctx.mode, ctx.apiBase, uid] as const,
    queryFn: () => apiGet<LapResource>(ctx, `/api/1/laps/${uid}`),
    enabled: !!uid,
    staleTime: 60_000,
  });
}

export function useSession(uid: string | undefined) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.session(ctx, uid),
    queryFn: () => apiGet<SessionResource>(ctx, `/api/1/sessions/${uid}`),
    enabled: !!uid,
  });
}

export function useSessionLaps(sessionUid: string | undefined, paging: PagingAndSort = {}) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.laps(ctx, { sessionUid, ...paging }),
    queryFn: () =>
      apiGet<Page<LapResource>>(
        ctx,
        `/api/1/laps${buildQuery({
          sessionUid,
          page: paging.page ?? 1,
          size: paging.size ?? 200,
          sort: paging.sort ?? "lapNumber:ASC",
        })}`,
      ),
    enabled: !!sessionUid,
  });
}

export function useLapTelemetry(lapUid: string | undefined) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.lapTelemetry(ctx, lapUid),
    queryFn: () => apiGet<LapTelemetryResource>(ctx, `/api/1/laps/${lapUid}/telemetry`),
    enabled: !!lapUid,
    // Telemetry is immutable for a given lap — keep it cached longer.
    staleTime: 5 * 60_000,
  });
}

export function useLapComparison(lap1Uid: string | undefined, lap2Uid: string | undefined) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.compare(ctx, lap1Uid, lap2Uid),
    queryFn: () =>
      apiGet<LapComparisonResource>(
        ctx,
        `/api/1/laps/compare?lap1Uid=${lap1Uid}&lap2Uid=${lap2Uid}`,
      ),
    enabled: !!lap1Uid && !!lap2Uid,
    staleTime: 5 * 60_000,
  });
}

export interface LapFilters {
  /** Integer car number within the session. */
  carId?: number;
  validLap?: boolean;
  personalBest?: boolean;
  /** Owning-session car name. Backend joins SESSION when set. */
  car?: string;
  /** Owning-session track name. Backend joins SESSION when set. */
  track?: string;
  /** Owning-session simulator. Backend joins SESSION when set. */
  simulator?: string;
  /** Inclusive lower bound on lap.recordedAt as ISO-8601 instant. */
  from?: string;
  /** Inclusive upper bound on lap.recordedAt as ISO-8601 instant. */
  to?: string;
}

/**
 * Returns the fastest valid lap of the supplied session, or `undefined` while
 * loading / when the session has no valid laps yet. Cheap — `size=1` with a
 * server-side sort.
 */
export function useSessionBestLap(sessionUid: string | undefined) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: ["session-best-lap", ctx.mode, ctx.apiBase, sessionUid] as const,
    queryFn: async () => {
      const page = await apiGet<Page<LapResource>>(
        ctx,
        `/api/1/laps${buildQuery({
          sessionUid,
          validLap: true,
          page: 1,
          size: 1,
          sort: "lapTime:ASC",
        })}`,
      );
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
export function useTrackBestLap(track: string | null | undefined) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: ["track-best-lap", ctx.mode, ctx.apiBase, track] as const,
    queryFn: async () => {
      const page = await apiGet<Page<LapResource>>(
        ctx,
        `/api/1/laps${buildQuery({
          track: track ?? undefined,
          validLap: true,
          page: 1,
          size: 1,
          sort: "lapTime:ASC",
        })}`,
      );
      return page.items[0] ?? null;
    },
    enabled: !!track,
    staleTime: 60_000,
  });
}

export function useLaps(params: LapFilters & PagingAndSort = {}) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.laps(ctx, params),
    queryFn: () =>
      apiGet<Page<LapResource>>(
        ctx,
        `/api/1/laps${buildQuery({
          carId: params.carId,
          validLap: params.validLap,
          personalBest: params.personalBest,
          car: params.car,
          track: params.track,
          simulator: params.simulator,
          from: params.from,
          to: params.to,
          page: params.page ?? 1,
          size: params.size ?? 50,
          sort: params.sort ?? "lapTime:ASC",
        })}`,
      ),
    placeholderData: (previous) => previous,
  });
}
