/**
 * In-browser mock dataset. Shapes mirror the JSON produced by the backend
 * (see `app/.../adapter/in/web/.../SessionResource.kt` and `LapResource.kt`)
 * and the values mirror what `DatabaseSeeder` would write — same cars, tracks,
 * simulators, session types, and lap-time distribution.
 *
 * If you change the seeder profiles, update this file to match.
 */
import type {
  LapResource,
  Page,
  SessionOptionsResource,
  SessionResource,
  TelemetrySample,
} from "../types";

interface Profile {
  simulator: "ACC" | "F1";
  track: string;
  car: string;
  /** Player's car number within the session. */
  carId: number;
  sessionType: string;
  lapCount: number;
  baseLapMs: number;
}

const PROFILES: Profile[] = [
  { simulator: "ACC", track: "Monza",             car: "Ferrari 488 GT3",      carId: 20, sessionType: "Practice",    lapCount: 12, baseLapMs: 107_000 },
  { simulator: "ACC", track: "Spa-Francorchamps", car: "Porsche 991 II GT3 R", carId: 23, sessionType: "Qualifying",  lapCount: 8,  baseLapMs: 136_500 },
  { simulator: "ACC", track: "Nurburgring",       car: "Mercedes-AMG GT3",     carId: 1,  sessionType: "Race",        lapCount: 18, baseLapMs: 114_200 },
  { simulator: "ACC", track: "Silverstone",       car: "Audi R8 LMS Evo",      carId: 31, sessionType: "Practice",    lapCount: 14, baseLapMs: 118_800 },
  { simulator: "ACC", track: "Snetterton",        car: "McLaren 720S GT3",     carId: 30, sessionType: "Race",        lapCount: 20, baseLapMs: 105_100 },
  { simulator: "ACC", track: "Brands Hatch",      car: "BMW M4 GT3",           carId: 30, sessionType: "Qualifying",  lapCount: 10, baseLapMs: 83_200  },
  { simulator: "F1",  track: "Monaco",            car: "F1 2026",              carId: 0,  sessionType: "Practice",    lapCount: 15, baseLapMs: 72_500  },
  { simulator: "F1",  track: "Suzuka",            car: "F1 2026",              carId: 0,  sessionType: "Race",        lapCount: 25, baseLapMs: 91_900  },
  { simulator: "ACC", track: "Monza",             car: "Porsche 991 II GT3 R", carId: 23, sessionType: "Race",        lapCount: 22, baseLapMs: 108_400 },
  { simulator: "ACC", track: "Spa-Francorchamps", car: "Ferrari 488 GT3",      carId: 20, sessionType: "Practice",    lapCount: 16, baseLapMs: 137_800 },
];

/** Pace offsets (ms) for competitor cars added to Race/Qualifying sessions. */
const COMPETITOR_OFFSETS = [+1_800, -900];

// Deterministic PRNG so mock data is stable across reloads.
function mulberry32(seed: number): () => number {
  let s = seed >>> 0;
  return () => {
    s |= 0;
    s = (s + 0x6d2b79f5) | 0;
    let t = Math.imul(s ^ (s >>> 15), 1 | s);
    t ^= t + Math.imul(t ^ (t >>> 7), 61 | t);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

const NOW = new Date("2026-05-03T12:00:00Z").getTime();
const DAY_MS = 86_400_000;

function uid(seed: number): string {
  // Deterministic 32-char hex — matches Uid format on the backend.
  const rand = mulberry32(seed);
  let out = "";
  for (let i = 0; i < 32; i++) out += Math.floor(rand() * 16).toString(16);
  return out;
}

function buildSessions(): SessionResource[] {
  return PROFILES.map((p, i) => {
    const sessionUid = uid(0x100 + i);
    const startedAt = new Date(NOW - (i + 1) * DAY_MS).toISOString();
    // Approximate the player's drivingTime as lapCount × baseLap. The real backend folds each
    // recorded lap time individually, but for mock data this is close enough for layout.
    const drivingTimeMs = p.lapCount * p.baseLapMs;
    return {
      uid: sessionUid,
      startedAt,
      simulator: p.simulator,
      track: p.track,
      car: p.car,
      sessionType: p.sessionType,
      playerCarId: p.carId,
      drivingTimeMs,
      _links: {
        self: `/api/1/sessions/${sessionUid}`,
        laps: `/api/1/laps?sessionUid=${sessionUid}`,
      },
    };
  });
}

function buildLapsFor(
  session: SessionResource,
  profile: Profile,
  carId: number,
  car: string | null,
  baseLapMs: number,
  seed: number,
): LapResource[] {
  const rand = mulberry32(seed);
  const start = new Date(session.startedAt!).getTime();
  let bestSoFar = Number.POSITIVE_INFINITY;
  let recordedAt = start;
  const out: LapResource[] = [];
  for (let lapIndex = 1; lapIndex <= profile.lapCount; lapIndex++) {
    const variation =
      lapIndex === 1
        ? Math.round(baseLapMs / 12)
        : Math.round((rand() - 0.4) * 3500);
    const lapTime = baseLapMs + variation;
    const valid = Math.floor(rand() * 20) !== 0;
    const personalBest = valid && lapTime < bestSoFar;
    if (personalBest) bestSoFar = lapTime;
    recordedAt += lapTime;
    const lapUid = uid(seed * 1000 + lapIndex);
    out.push({
      uid: lapUid,
      sessionUid: session.uid,
      carId,
      car,
      track: session.track,
      playerLap: session.playerCarId !== null ? carId === session.playerCarId : null,
      recordedAt: new Date(recordedAt).toISOString(),
      lapTime,
      lapNumber: lapIndex,
      valid,
      personalBest,
      _links: {
        self: `/api/1/laps/${lapUid}`,
        session: `/api/1/sessions/${session.uid}`,
        telemetry: `/api/1/laps/${lapUid}/telemetry`,
      },
    });
  }
  return out;
}

export const SESSIONS: SessionResource[] = buildSessions();

export const LAPS: LapResource[] = SESSIONS.flatMap((s, i) => {
  const profile = PROFILES[i]!;
  const playerLaps = buildLapsFor(s, profile, profile.carId, profile.car, profile.baseLapMs, 0x200 + i);

  // Add competitor laps for Race/Qualifying sessions.
  const competitorLaps =
    profile.sessionType === "Race" || profile.sessionType === "Qualifying"
      ? COMPETITOR_OFFSETS.flatMap((offsetMs, cIdx) => {
          const compCarId = profile.carId + cIdx + 1;
          const compBase = profile.baseLapMs + offsetMs;
          return buildLapsFor(s, profile, compCarId, null, compBase, 0x400 + i * 10 + cIdx);
        })
      : [];

  return [...playerLaps, ...competitorLaps];
});

const SAMPLES_PER_LAP = 150;

const TRACK_CORNERS: Record<string, number> = {
  Monza: 11,
  "Spa-Francorchamps": 19,
  Nurburgring: 16,
  Silverstone: 18,
  Snetterton: 12,
  "Brands Hatch": 9,
  Monaco: 19,
  Suzuka: 18,
};

/**
 * Generate a synthetic telemetry trace for a single lap. Mirrors the backend
 * seeder's algorithm so that mock-mode and live-mode look broadly similar.
 *
 * worldPosX/worldPosY use the same parametric ellipse + harmonic perturbation
 * that the backend seeder uses, keyed to cornerCount so each track has a
 * recognisably different shape.
 */
function buildTelemetry(track: string, baseLapMs: number, lapTimeMs: number, lapNumber: number): TelemetrySample[] {
  const cornerCount = TRACK_CORNERS[track] ?? 8;
  const paceFactor = 1 + (baseLapMs - lapTimeMs) / 50_000;
  const lapJitter = ((lapNumber * 17) % 7) * 0.4;
  const trackSeed = baseLapMs % 31;

  const radiusX = 500;
  const radiusY = 300;
  const perturbAmp = 80;
  const perturbPhase = trackSeed * 0.2;

  const speeds: number[] = [];
  for (let i = 0; i < SAMPLES_PER_LAP; i++) {
    const t = i / SAMPLES_PER_LAP;
    const cornerWave = Math.sin(2 * Math.PI * cornerCount * t + trackSeed * 0.1);
    const secondaryWave = Math.cos(4 * Math.PI * t + lapJitter);
    const baseline = 180 + 60 * cornerWave + 25 * secondaryWave;
    speeds.push(Math.max(60, baseline * paceFactor));
  }

  return speeds.map((speed, i): TelemetrySample => {
    let gear = 6;
    if (speed < 90) gear = 2;
    else if (speed < 130) gear = 3;
    else if (speed < 170) gear = 4;
    else if (speed < 210) gear = 5;
    const splinePosition = i / SAMPLES_PER_LAP;
    const angle = 2 * Math.PI * splinePosition;
    const perturb = perturbAmp * Math.sin(cornerCount * angle + perturbPhase);
    return {
      splinePosition,
      speedKph: speed,
      gear,
      worldPosX: (radiusX + perturb) * Math.cos(angle),
      worldPosY: (radiusY + perturb * 0.5) * Math.sin(angle),
    };
  });
}

/**
 * Map of `lapUid → telemetry samples`. Built lazily on first access in the
 * mock handler — generating ~26k samples eagerly costs ~30ms on cold load.
 */
export const TELEMETRY_BY_LAP_UID: Map<string, TelemetrySample[]> = new Map(
  LAPS.map((lap, idx) => {
    // Find the profile the lap belongs to via its session's track.
    const session = SESSIONS.find((s) => s.uid === lap.sessionUid)!;
    const profile = PROFILES.find(
      (p) => p.track === session.track && p.car === session.car,
    ) ?? PROFILES[0]!;
    return [
      lap.uid,
      buildTelemetry(profile.track, profile.baseLapMs, lap.lapTime, idx + 1),
    ] as const;
  }),
);

export const OPTIONS: SessionOptionsResource = {
  cars: Array.from(new Set(SESSIONS.map((s) => s.car!).filter(Boolean))).sort(),
  tracks: Array.from(new Set(SESSIONS.map((s) => s.track!).filter(Boolean))).sort(),
  simulators: Array.from(new Set(SESSIONS.map((s) => s.simulator))).sort(),
  minDate: SESSIONS.reduce<string>(
    (acc, s) => (s.startedAt && s.startedAt < acc ? s.startedAt : acc),
    SESSIONS[0]?.startedAt ?? new Date(NOW).toISOString(),
  ),
  maxDate: SESSIONS.reduce<string>(
    (acc, s) => (s.startedAt && s.startedAt > acc ? s.startedAt : acc),
    SESSIONS[0]?.startedAt ?? new Date(NOW).toISOString(),
  ),
  _links: {
    self: "/api/1/sessions/options",
    sessions: "/api/1/sessions",
  },
};

export function paged<T>(items: T[], page: number, size: number): Page<T> {
  const start = (page - 1) * size;
  return {
    page: { page, size },
    total: items.length,
    items: items.slice(start, start + size),
  };
}
