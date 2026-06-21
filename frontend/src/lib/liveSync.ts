/**
 * Pure mapping from a live WebSocket event type to the TanStack Query key prefixes that must be
 * invalidated. Kept free of React/DOM so it is unit-testable in the node test env.
 *
 * Prefixes match the first element of the `queryKey` tuples in `api/queries.ts`; TanStack does
 * partial (prefix) matching, so `["laps"]` invalidates every laps list regardless of its
 * filter/paging suffix.
 *
 * `PlayerCarUpdated` maps to no prefixes on purpose — it is high-frequency telemetry consumed
 * only by LiveScreen-local state, and invalidating on it would storm the cache.
 */
import type { WsMessage } from "../providers/LiveEventsProvider";

export const LAP_PREFIXES = [
  "laps",
  "laps-aggregate",
  "session-laps",
  "session-best-lap",
  "track-best-lap",
] as const;

export const SESSION_PREFIXES = ["sessions", "sessions-aggregate", "session"] as const;

/**
 * Whether the shared live socket should connect: only in LIVE data mode when the backend
 * advertises a `live` rel. MOCK mode and feature-off backends open no socket.
 */
export function shouldConnect(mode: string, liveLink: string | undefined): boolean {
  return mode === "live" && !!liveLink;
}

/** Query-key prefixes to invalidate for a given event type. Empty ⇒ no cache effect. */
export function invalidationPrefixesFor(type: WsMessage["type"]): readonly string[] {
  switch (type) {
    case "LapCreated":
      return LAP_PREFIXES;
    case "SessionCreated":
    case "SessionStarted":
    case "SessionUpdated":
    case "SessionEnded":
      return SESSION_PREFIXES;
    default:
      return [];
  }
}
