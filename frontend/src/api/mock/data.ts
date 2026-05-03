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
} from "../types";

interface Profile {
  simulator: "ACC" | "F1";
  track: string;
  car: string;
  sessionType: string;
  lapCount: number;
  baseLapMs: number;
}

const PROFILES: Profile[] = [
  { simulator: "ACC", track: "Monza",             car: "Ferrari 488 GT3",      sessionType: "Practice",    lapCount: 12, baseLapMs: 107_000 },
  { simulator: "ACC", track: "Spa-Francorchamps", car: "Porsche 991 II GT3 R", sessionType: "Qualifying",  lapCount: 8,  baseLapMs: 136_500 },
  { simulator: "ACC", track: "Nurburgring",       car: "Mercedes-AMG GT3",     sessionType: "Race",        lapCount: 18, baseLapMs: 114_200 },
  { simulator: "ACC", track: "Silverstone",       car: "Audi R8 LMS Evo",      sessionType: "Practice",    lapCount: 14, baseLapMs: 118_800 },
  { simulator: "ACC", track: "Snetterton",        car: "McLaren 720S GT3",     sessionType: "Race",        lapCount: 20, baseLapMs: 105_100 },
  { simulator: "ACC", track: "Brands Hatch",      car: "BMW M4 GT3",           sessionType: "Qualifying",  lapCount: 10, baseLapMs: 83_200  },
  { simulator: "F1",  track: "Monaco",            car: "F1 2026",              sessionType: "Practice",    lapCount: 15, baseLapMs: 72_500  },
  { simulator: "F1",  track: "Suzuka",            car: "F1 2026",              sessionType: "Race",        lapCount: 25, baseLapMs: 91_900  },
  { simulator: "ACC", track: "Monza",             car: "Porsche 991 II GT3 R", sessionType: "Race",        lapCount: 22, baseLapMs: 108_400 },
  { simulator: "ACC", track: "Spa-Francorchamps", car: "Ferrari 488 GT3",      sessionType: "Practice",    lapCount: 16, baseLapMs: 137_800 },
];

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
    const endedAt = new Date(
      NOW - (i + 1) * DAY_MS + p.lapCount * p.baseLapMs,
    ).toISOString();
    return {
      uid: sessionUid,
      startedAt,
      endedAt,
      simulator: p.simulator,
      track: p.track,
      car: p.car,
      sessionType: p.sessionType,
      _links: { self: `/api/1/sessions/${sessionUid}` },
    };
  });
}

function buildLapsFor(
  session: SessionResource,
  profile: Profile,
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
        ? Math.round(profile.baseLapMs / 12)
        : Math.round((rand() - 0.4) * 3500);
    const lapTime = profile.baseLapMs + variation;
    const valid = Math.floor(rand() * 20) !== 0;
    const personalBest = valid && lapTime < bestSoFar;
    if (personalBest) bestSoFar = lapTime;
    recordedAt += lapTime;
    const lapUid = uid(seed * 1000 + lapIndex);
    out.push({
      uid: lapUid,
      sessionUid: session.uid,
      recordedAt: new Date(recordedAt).toISOString(),
      lapTime,
      lapNumber: lapIndex,
      valid,
      personalBest,
      _links: { self: `/api/1/laps/${lapUid}` },
    });
  }
  return out;
}

export const SESSIONS: SessionResource[] = buildSessions();

export const LAPS: LapResource[] = SESSIONS.flatMap((s, i) =>
  buildLapsFor(s, PROFILES[i]!, 0x200 + i),
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
  _links: { self: "/api/1/sessions/options" },
};

export function paged<T>(items: T[], page: number, size: number): Page<T> {
  const start = (page - 1) * size;
  return {
    page: { page, size },
    total: items.length,
    items: items.slice(start, start + size),
  };
}
