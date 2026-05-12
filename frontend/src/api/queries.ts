/**
 * TanStack Query hooks — HATEOAS first.
 *
 * Every URL the hooks fetch comes from a link relation rather than a hard-coded path. Listing /
 * search hooks follow the rels exposed by `GET /api/1` (`useIndexLink`); per-record hooks follow
 * the rels on the parent record itself (e.g. `useSessionLaps(session)` follows
 * `session._links.laps`). If the backend stops advertising a rel — whether because the feature
 * was disabled or the record genuinely has no associated child — the hook short-circuits to
 * `enabled: false` and never fires a request.
 *
 * The "by uid" entry-point hooks (`useSession(uid)`, `useLap(uid)`) are a small pragmatic
 * exception: there is no parent record when you only have a uid, so the path is composed from
 * the index's `sessions` / `laps` rel + `/{uid}`. The frontend still doesn't carry hard-coded
 * `/api/1/...` strings.
 */
import { useQuery } from "@tanstack/react-query";
import { apiGet, appendQuery, type ApiContext } from "./client";
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
import { useFeatures } from "../providers/FeaturesProvider";

function useApiContext(): ApiContext {
  const { mode, apiUrl } = useDataMode();
  return { mode, apiBase: apiUrl };
}

/** Look up a rel from the bootstrap index `/api/1` `_links`. `undefined` ⇒ feature is off. */
function useIndexLink(rel: string): string | undefined {
  const href = useFeatures().links[rel];
  return href ? href : undefined;
}

// ─── Listings (index-link driven) ────────────────────────────────────────────

export function useSessionOptions() {
  const ctx = useApiContext();
  const href = useIndexLink("sessionOptions");
  return useQuery({
    queryKey: ["session-options", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<SessionOptionsResource>(ctx, href!),
    enabled: !!href,
  });
}

export function useSessions(filters: SessionFilters & PagingAndSort = {}) {
  const ctx = useApiContext();
  const base = useIndexLink("sessions");
  const href =
    base &&
    appendQuery(base, {
      ...filters,
      page: filters.page ?? 1,
      size: filters.size ?? 25,
      sort: filters.sort ?? "startedAt:DESC",
    });
  return useQuery({
    queryKey: ["sessions", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<Page<SessionResource>>(ctx, href!),
    enabled: !!href,
    placeholderData: (previous) => previous,
  });
}

export interface LapFilters {
  /** Integer car number within the session. */
  carId?: number;
  validLap?: boolean;
  personalBest?: boolean;
  /** If true, restrict to laps recorded by the player's car in the owning session. */
  playerLap?: boolean;
  /**
   * If true, post-filter the matched rows to keep only the fastest lap per `track`.
   * Pair with `playerLap: true` and `validLap: true` for the player's all-time best per track.
   */
  allTimeBest?: boolean;
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

export type LapAggregateGroupBy = "track" | "day" | "week" | "month";

export interface LapAggregateBucket {
  key: string;
  count: number;
}

export interface LapAggregateResource {
  groupBy: LapAggregateGroupBy;
  buckets: LapAggregateBucket[];
}

/**
 * Server-side `COUNT(*) GROUP BY` over laps. The same filter set as `useLaps`; pair with
 * `playerLap: true` for "player laps per …" charts. Result is sparse — the caller fills any
 * zero-count buckets it needs for layout.
 */
export function useLapAggregate(
  params: { groupBy: LapAggregateGroupBy } & LapFilters,
) {
  const ctx = useApiContext();
  const base = useIndexLink("lapsAggregate");
  const href =
    base &&
    appendQuery(base, {
      groupBy: params.groupBy,
      carId: params.carId,
      validLap: params.validLap,
      personalBest: params.personalBest,
      playerLap: params.playerLap,
      allTimeBest: params.allTimeBest,
      car: params.car,
      track: params.track,
      simulator: params.simulator,
      from: params.from,
      to: params.to,
    });
  return useQuery({
    queryKey: ["laps-aggregate", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<LapAggregateResource>(ctx, href!),
    enabled: !!href,
    placeholderData: (previous) => previous,
  });
}

export function useLaps(params: LapFilters & PagingAndSort = {}) {
  const ctx = useApiContext();
  const base = useIndexLink("laps");
  const href =
    base &&
    appendQuery(base, {
      ...params,
      page: params.page ?? 1,
      size: params.size ?? 50,
      sort: params.sort ?? "lapTime:ASC",
    });
  return useQuery({
    queryKey: ["laps", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<Page<LapResource>>(ctx, href!),
    enabled: !!href,
    placeholderData: (previous) => previous,
  });
}

/**
 * Returns the fastest valid lap recorded at the supplied track across every
 * session — i.e. the all-time PB at that track.
 */
export function useTrackBestLap(track: string | null | undefined) {
  const ctx = useApiContext();
  const base = useIndexLink("laps");
  const href =
    base && track
      ? appendQuery(base, {
          track,
          validLap: true,
          page: 1,
          size: 1,
          sort: "lapTime:ASC",
        })
      : undefined;
  return useQuery({
    queryKey: ["track-best-lap", ctx.mode, ctx.apiBase, href] as const,
    queryFn: async () => {
      const page = await apiGet<Page<LapResource>>(ctx, href!);
      return page.items[0] ?? null;
    },
    enabled: !!href,
    staleTime: 60_000,
  });
}

// ─── Entry-points by uid (compose `${indexLinks.x}/{uid}`) ───────────────────

export function useSession(uid: string | undefined) {
  const ctx = useApiContext();
  const base = useIndexLink("sessions");
  const href = base && uid ? `${base}/${uid}` : undefined;
  return useQuery({
    queryKey: ["session", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<SessionResource>(ctx, href!),
    enabled: !!href,
  });
}

export function useLap(uid: string | undefined) {
  const ctx = useApiContext();
  const base = useIndexLink("laps");
  const href = base && uid ? `${base}/${uid}` : undefined;
  return useQuery({
    queryKey: ["lap", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<LapResource>(ctx, href!),
    enabled: !!href,
    staleTime: 60_000,
  });
}

// ─── Per-record link followers ──────────────────────────────────────────────

/** Follows `session._links.laps`. Returns nothing if the rel is absent. */
export function useSessionLaps(
  session: SessionResource | undefined,
  paging: PagingAndSort = {},
) {
  const ctx = useApiContext();
  const base = session?._links.laps;
  const href =
    base &&
    appendQuery(base, {
      page: paging.page ?? 1,
      size: paging.size ?? 200,
      sort: paging.sort ?? "lapNumber:ASC",
    });
  return useQuery({
    queryKey: ["session-laps", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<Page<LapResource>>(ctx, href!),
    enabled: !!href,
  });
}

/** Session-best lap follows `session._links.laps` filtered to the fastest valid lap. */
export function useSessionBestLap(session: SessionResource | undefined) {
  const ctx = useApiContext();
  const base = session?._links.laps;
  const href =
    base &&
    appendQuery(base, {
      validLap: true,
      page: 1,
      size: 1,
      sort: "lapTime:ASC",
    });
  return useQuery({
    queryKey: ["session-best-lap", ctx.mode, ctx.apiBase, href] as const,
    queryFn: async () => {
      const page = await apiGet<Page<LapResource>>(ctx, href!);
      return page.items[0] ?? null;
    },
    enabled: !!href,
    staleTime: 60_000,
  });
}

/** Follows `lap._links.telemetry`. */
export function useLapTelemetry(lap: LapResource | undefined) {
  const ctx = useApiContext();
  const href = lap?._links.telemetry;
  return useQuery({
    queryKey: ["lap-telemetry", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<LapTelemetryResource>(ctx, href!),
    enabled: !!href,
    staleTime: 5 * 60_000,
  });
}

/**
 * Compare endpoint is not anchored to any one resource — there is no `compare` rel on a lap,
 * since "compare lap A with lap B" is a binary relation. Use the index `compare` rel + the two
 * uids.
 */
export function useLapComparison(lap1Uid: string | undefined, lap2Uid: string | undefined) {
  const ctx = useApiContext();
  const base = useIndexLink("compare");
  const href =
    base && lap1Uid && lap2Uid ? `${base}?lap1Uid=${lap1Uid}&lap2Uid=${lap2Uid}` : undefined;
  return useQuery({
    queryKey: ["compare-laps", ctx.mode, ctx.apiBase, href] as const,
    queryFn: () => apiGet<LapComparisonResource>(ctx, href!),
    enabled: !!href,
    staleTime: 5 * 60_000,
  });
}
