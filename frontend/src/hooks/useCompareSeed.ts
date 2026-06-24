import { useMemo } from "react";
import { useSessions, useSessionLaps, useTrackBestLap } from "../api/queries";
import type { LapResource, SessionResource } from "../api/types";

/**
 * Result of seeding the Compare screen from the user's latest session.
 *
 * The Compare screen needs to be useful with zero clicks, so on a fresh landing
 * we derive a comparison track and a default anchor lap from the latest session
 * rather than forcing the user to pick. The values are *derived*, not written to
 * the URL — an explicit `track`/`anchor` URL param always wins (see
 * `effectiveTrack` / `defaultAnchorUid` being `undefined`-deferred to the param
 * in `CompareScreen`).
 */
export interface CompareSeed {
  /** The latest session, or undefined while loading / when none exist. */
  latestSession: SessionResource | undefined;
  /**
   * Track to use as the comparison axis when no `track` param is set —
   * the latest session's track.
   */
  seedTrack: string | undefined;
  /**
   * Default anchor lap for `track`: the player's fastest valid lap in the
   * latest session when that session is on `track`, else the session's best
   * (fastest valid lap, any driver), else the all-time track best. Undefined
   * while loading or when no lap qualifies.
   */
  defaultAnchor: LapResource | undefined;
  /** Car to seed the challenger "Same car" filter from (anchor's car, else session car). */
  seedCar: string | undefined;
}

/**
 * Derives the latest-session seed for the Compare screen.
 *
 * `track` is the *effective* comparison track (the `track` URL param, or the
 * latest session's track when the param is absent). It drives which laps qualify
 * as the default anchor so the seed stays correct after the user changes tracks.
 */
export function useCompareSeed(track: string | undefined): CompareSeed {
  const latestSessionsQuery = useSessions({ sort: "startedAt:DESC", size: 1 });
  const latestSession = latestSessionsQuery.data?.items?.[0];

  const seedTrack = latestSession?.track ?? undefined;
  const effectiveTrack = track ?? seedTrack;

  // Latest-session laps, fastest first, so we can pick the player's best (or the
  // session best) without a dedicated endpoint.
  const sessionLapsQuery = useSessionLaps(latestSession, {
    sort: "lapTime:ASC",
    size: 200,
  });

  // Fallback when the comparison track differs from the latest session's track
  // (e.g. the user switched tracks): the all-time best valid lap at that track.
  const trackBestQuery = useTrackBestLap(effectiveTrack ?? null);

  const defaultAnchor = useMemo<LapResource | undefined>(() => {
    const onSeedTrack = !!effectiveTrack && latestSession?.track === effectiveTrack;
    if (onSeedTrack) {
      const laps = sessionLapsQuery.data?.items ?? [];
      const valid = laps.filter((l) => l.valid && l.track === effectiveTrack);
      const mine = valid.find((l) => l.playerLap === true);
      // mine → player's fastest in the session; else the session best.
      const fromSession = mine ?? valid[0];
      if (fromSession) return fromSession;
    }
    return trackBestQuery.data ?? undefined;
  }, [effectiveTrack, latestSession?.track, sessionLapsQuery.data, trackBestQuery.data]);

  const seedCar = defaultAnchor?.car ?? latestSession?.car ?? undefined;

  return { latestSession, seedTrack, defaultAnchor, seedCar };
}
