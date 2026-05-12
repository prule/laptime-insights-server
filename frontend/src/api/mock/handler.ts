/**
 * Routes a relative API path to the matching mock dataset.
 *
 * Returns `undefined` when no handler matches, which the caller turns into a
 * 404. Adding a new endpoint? Add a branch here that mirrors the Ktor route.
 */
import { LAPS, OPTIONS, SESSIONS, TELEMETRY_BY_LAP_UID, paged } from "./data";
import type {
  LapComparisonResource,
  LapResource,
  LapTelemetryResource,
  Page,
  SessionResource,
  SessionOptionsResource,
} from "../types";

const SIMULATE_LATENCY_MS = 120;

function delay<T>(value: T): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), SIMULATE_LATENCY_MS));
}

function parsePath(path: string): { pathname: string; query: URLSearchParams } {
  // Treat everything after `?` as the query. Use a fake origin so URL parses.
  const url = new URL(path, "http://mock.local");
  return { pathname: url.pathname, query: url.searchParams };
}

function pickPagingAndSort(query: URLSearchParams): { page: number; size: number } {
  const page = Number.parseInt(query.get("page") ?? "1", 10) || 1;
  const size = Number.parseInt(query.get("size") ?? "25", 10) || 25;
  return { page, size };
}

function compareSessions(sort: string | null) {
  if (!sort) return (_a: SessionResource, _b: SessionResource) => 0;
  // Support a single primary sort key: `field:ASC|DESC`.
  const [field, order] = sort.split(",")[0]!.split(":");
  const direction = (order ?? "ASC").toUpperCase() === "DESC" ? -1 : 1;
  return (a: SessionResource, b: SessionResource): number => {
    const av = (a as unknown as Record<string, unknown>)[field!] ?? "";
    const bv = (b as unknown as Record<string, unknown>)[field!] ?? "";
    return av < bv ? -1 * direction : av > bv ? 1 * direction : 0;
  };
}

/**
 * Mirror the backend's `formatBucketKey` — UTC date string for day/week, UTC `YYYY-MM` for month,
 * lap.track for track. Week uses Monday as start of week (ISO).
 */
function aggregateKey(lap: LapResource, groupBy: "track" | "day" | "week" | "month"): string {
  if (groupBy === "track") return lap.track ?? "";
  const d = new Date(lap.recordedAt);
  const y = d.getUTCFullYear();
  const m = String(d.getUTCMonth() + 1).padStart(2, "0");
  if (groupBy === "month") return `${y}-${m}`;
  if (groupBy === "day") {
    const dd = String(d.getUTCDate()).padStart(2, "0");
    return `${y}-${m}-${dd}`;
  }
  // ISO week start (Monday) in UTC.
  const dayOfWeek = (d.getUTCDay() + 6) % 7; // 0=Mon … 6=Sun
  const monday = new Date(
    Date.UTC(d.getUTCFullYear(), d.getUTCMonth(), d.getUTCDate() - dayOfWeek),
  );
  const wy = monday.getUTCFullYear();
  const wm = String(monday.getUTCMonth() + 1).padStart(2, "0");
  const wd = String(monday.getUTCDate()).padStart(2, "0");
  return `${wy}-${wm}-${wd}`;
}

function compareLaps(sort: string | null) {
  if (!sort) return (a: LapResource, b: LapResource) => a.lapNumber - b.lapNumber;
  const [field, order] = sort.split(",")[0]!.split(":");
  const direction = (order ?? "ASC").toUpperCase() === "DESC" ? -1 : 1;
  return (a: LapResource, b: LapResource): number => {
    const av = (a as unknown as Record<string, unknown>)[field!] ?? 0;
    const bv = (b as unknown as Record<string, unknown>)[field!] ?? 0;
    return av < bv ? -1 * direction : av > bv ? 1 * direction : 0;
  };
}

export async function mockHandler(path: string): Promise<unknown> {
  const { pathname, query } = parsePath(path);

  if (pathname === "/api/1") {
    // Mock mode mirrors a fully-featured backend so every feature is on. Real-mode toggling is
    // driven by the Ktor `FEATURE_<NAME>` env vars.
    return delay({
      _links: {
        self: "/api/1",
        overview: "/api/1/sessions",
        sessions: "/api/1/sessions",
        sessionOptions: "/api/1/sessions/options",
        laps: "/api/1/laps",
        lapsAggregate: "/api/1/laps/aggregate",
        compare: "/api/1/laps/compare",
        live: "/api/1/events",
      },
    });
  }

  if (pathname === "/api/1/sessions/options") {
    return delay<SessionOptionsResource>(OPTIONS);
  }

  if (pathname === "/api/1/sessions") {
    const { page, size } = pickPagingAndSort(query);
    let items = SESSIONS.slice();
    const car = query.get("car");
    const track = query.get("track");
    const simulator = query.get("simulator");
    const from = query.get("from");
    const to = query.get("to");
    const uid = query.get("uid");
    if (uid) items = items.filter((s) => s.uid === uid);
    if (car) items = items.filter((s) => s.car === car);
    if (track) items = items.filter((s) => s.track === track);
    if (simulator) items = items.filter((s) => s.simulator === simulator);
    if (from) items = items.filter((s) => (s.startedAt ?? "") >= from);
    if (to) items = items.filter((s) => (s.startedAt ?? "") <= to);
    items = items.sort(compareSessions(query.get("sort") ?? "startedAt:DESC"));
    return delay<Page<SessionResource>>(paged(items, page, size));
  }

  const sessionMatch = pathname.match(/^\/api\/1\/sessions\/([^/]+)$/);
  if (sessionMatch) {
    const session = SESSIONS.find((s) => s.uid === sessionMatch[1]);
    if (!session) return undefined;
    return delay(session);
  }

  // /api/1/laps/aggregate must be matched before the /api/1/laps/{uid} pattern.
  if (pathname === "/api/1/laps/aggregate") {
    const groupBy = (query.get("groupBy") ?? "") as "track" | "day" | "week" | "month";
    if (!["track", "day", "week", "month"].includes(groupBy)) return undefined;
    // Mirror the backend lap filters (subset that matters for the dashboards).
    let items = LAPS.slice();
    const playerLap = query.get("playerLap");
    const validLap = query.get("validLap");
    const from = query.get("from");
    const to = query.get("to");
    const track = query.get("track");
    if (playerLap === "true") items = items.filter((l) => l.playerLap === true);
    if (playerLap === "false") items = items.filter((l) => l.playerLap === false);
    if (validLap === "true") items = items.filter((l) => l.valid);
    if (validLap === "false") items = items.filter((l) => !l.valid);
    if (from) items = items.filter((l) => l.recordedAt >= from);
    if (to) items = items.filter((l) => l.recordedAt <= to);
    if (track) items = items.filter((l) => l.track === track);
    if (groupBy === "track") items = items.filter((l) => !!l.track);
    const counts = new Map<string, number>();
    for (const l of items) {
      const key = aggregateKey(l, groupBy);
      counts.set(key, (counts.get(key) ?? 0) + 1);
    }
    const buckets = Array.from(counts, ([key, count]) => ({ key, count }));
    return delay({ groupBy, buckets });
  }

  // /api/1/laps/compare must be matched before the /api/1/laps/{uid} pattern.
  if (pathname === "/api/1/laps/compare") {
    const lap1Uid = query.get("lap1Uid");
    const lap2Uid = query.get("lap2Uid");
    if (!lap1Uid || !lap2Uid) return undefined;
    const lap1 = LAPS.find((l) => l.uid === lap1Uid);
    const lap2 = LAPS.find((l) => l.uid === lap2Uid);
    if (!lap1 || !lap2) return undefined;
    const sideOf = (lap: LapResource) => ({
      lapUid: lap.uid,
      sessionUid: lap.sessionUid,
      lapNumber: lap.lapNumber,
      lapTimeMs: lap.lapTime,
      valid: lap.valid,
      personalBest: lap.personalBest,
      samples: TELEMETRY_BY_LAP_UID.get(lap.uid) ?? [],
    });
    return delay<LapComparisonResource>({
      lap1: sideOf(lap1),
      lap2: sideOf(lap2),
      _links: {
        self: `/api/1/laps/compare?lap1Uid=${lap1Uid}&lap2Uid=${lap2Uid}`,
        lap1: `/api/1/laps/${lap1Uid}`,
        lap2: `/api/1/laps/${lap2Uid}`,
      },
    });
  }

  const telemetryMatch = pathname.match(/^\/api\/1\/laps\/([^/]+)\/telemetry$/);
  if (telemetryMatch) {
    const lapUid = telemetryMatch[1]!;
    if (!LAPS.some((l) => l.uid === lapUid)) return undefined;
    return delay<LapTelemetryResource>({
      lapUid,
      samples: TELEMETRY_BY_LAP_UID.get(lapUid) ?? [],
      _links: {
        self: `/api/1/laps/${lapUid}/telemetry`,
        lap: `/api/1/laps/${lapUid}`,
      },
    });
  }

  if (pathname === "/api/1/laps") {
    const { page, size } = pickPagingAndSort(query);
    let items = LAPS.slice();
    const sessionUid = query.get("sessionUid");
    const carIdParam = query.get("carId");
    const personalBest = query.get("personalBest");
    const validLap = query.get("validLap");
    const playerLap = query.get("playerLap");
    const allTimeBest = query.get("allTimeBest");
    const uid = query.get("uid");
    const car = query.get("car");
    const track = query.get("track");
    const simulator = query.get("simulator");
    if (uid) items = items.filter((l) => l.uid === uid);
    if (sessionUid) items = items.filter((l) => l.sessionUid === sessionUid);
    if (carIdParam !== null) {
      const carIdNum = Number.parseInt(carIdParam, 10);
      items = items.filter((l) => l.carId === carIdNum);
    }
    if (personalBest === "true") items = items.filter((l) => l.personalBest);
    if (personalBest === "false") items = items.filter((l) => !l.personalBest);
    if (validLap === "true") items = items.filter((l) => l.valid);
    if (validLap === "false") items = items.filter((l) => !l.valid);
    if (playerLap === "true") items = items.filter((l) => l.playerLap === true);
    if (playerLap === "false") items = items.filter((l) => l.playerLap === false);
    if (car || track || simulator) {
      // Mirror the backend's SESSION join: filter laps by their owning
      // session's car / track / simulator.
      const sessionsByUid = new Map(SESSIONS.map((s) => [s.uid, s] as const));
      items = items.filter((lap) => {
        const session = sessionsByUid.get(lap.sessionUid);
        if (!session) return false;
        if (car && session.car !== car) return false;
        if (track && session.track !== track) return false;
        if (simulator && session.simulator !== simulator) return false;
        return true;
      });
    }
    if (allTimeBest === "true") {
      // Mirror backend: keep the fastest lap per `track`, drop null-track laps.
      const bestByTrack = new Map<string, LapResource>();
      for (const l of items) {
        if (!l.track) continue;
        const cur = bestByTrack.get(l.track);
        if (!cur || l.lapTime < cur.lapTime) bestByTrack.set(l.track, l);
      }
      items = Array.from(bestByTrack.values());
    }
    items = items.sort(compareLaps(query.get("sort")));
    return delay<Page<LapResource>>(paged(items, page, size));
  }

  const lapMatch = pathname.match(/^\/api\/1\/laps\/([^/]+)$/);
  if (lapMatch) {
    const lap = LAPS.find((l) => l.uid === lapMatch[1]);
    if (!lap) return undefined;
    return delay(lap);
  }

  return undefined;
}
