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
  LapResource,
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

export interface LapFilters {
  validLap?: boolean;
  personalBest?: boolean;
}

export function useLaps(params: LapFilters & PagingAndSort = {}) {
  const ctx = useApiContext();
  return useQuery({
    queryKey: KEYS.laps(ctx, params),
    queryFn: () =>
      apiGet<Page<LapResource>>(
        ctx,
        `/api/1/laps${buildQuery({
          validLap: params.validLap,
          personalBest: params.personalBest,
          page: params.page ?? 1,
          size: params.size ?? 50,
          sort: params.sort ?? "lapTime:ASC",
        })}`,
      ),
    placeholderData: (previous) => previous,
  });
}
