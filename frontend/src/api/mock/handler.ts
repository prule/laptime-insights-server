/**
 * Routes a relative API path to the matching mock dataset.
 *
 * Returns `undefined` when no handler matches, which the caller turns into a
 * 404. Adding a new endpoint? Add a branch here that mirrors the Ktor route.
 */
import { LAPS, OPTIONS, SESSIONS, paged } from "./data";
import type { LapResource, Page, SessionResource, SessionOptionsResource } from "../types";

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

  if (pathname === "/api/1/laps") {
    const { page, size } = pickPagingAndSort(query);
    let items = LAPS.slice();
    const sessionUid = query.get("sessionUid");
    const personalBest = query.get("personalBest");
    const validLap = query.get("validLap");
    const uid = query.get("uid");
    if (uid) items = items.filter((l) => l.uid === uid);
    if (sessionUid) items = items.filter((l) => l.sessionUid === sessionUid);
    if (personalBest === "true") items = items.filter((l) => l.personalBest);
    if (personalBest === "false") items = items.filter((l) => !l.personalBest);
    if (validLap === "true") items = items.filter((l) => l.valid);
    if (validLap === "false") items = items.filter((l) => !l.valid);
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
