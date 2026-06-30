/**
 * Public Profile (Driver Passport) data contract + bundled sample snapshot.
 *
 * `ProfileData` is the contract of record: it mirrors the JSON a paying user's local install will
 * later POST on a schedule, and that the server will host at a vanity URL. For now the page is
 * driven entirely by the bundled `SAMPLE_PROFILE` constant — no backend request. When the backend
 * starts serving this payload, `useProfileData()` becomes a data-mode-keyed fetch and nothing in
 * the screen/components needs to change.
 *
 * Ported from the design handoff's `profile-data.js` (field names kept identical on purpose).
 */
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { apiGet, apiSend, type ApiContext } from "./client";
import { useDataMode } from "../providers/DataModeProvider";
import { useFeatures } from "../providers/FeaturesProvider";

export interface ProfileMeta {
  slug: string;
  season: string;
  /** Human-readable date range covered by the snapshot, e.g. "Jan 1 – May 26, 2025". */
  range: string;
  /** ISO timestamp the snapshot was generated. */
  generatedAt: string;
  sim: string;
}

export interface Profile {
  name: string;
  slug: string;
  /** Derived from the name; shown on the license card. */
  initials: string;
  /** Optional free-text strapline entered at signup. */
  tagline: string;
  location: string;
  member_since: string;
}

export interface Totals {
  laps: number;
  distanceKm: number;
  hours: number;
  sessions: number;
  daysActive: number;
  /** Longest run of consecutive active days. */
  longestStreak: number;
  tracks: number;
  cars: number;
  /** Most-driven car this season (derived). */
  topCar: string;
}

export interface PerTrack {
  track: string;
  laps: number;
  /** Key into `TRACK_ART` for the circuit outline. */
  art: string;
  /** Accent colour for this circuit's stamp. */
  accent: string;
}

export interface RecordRow {
  track: string;
  car: string;
  season: string;
  allTime: string;
  allTimeWhen: string;
  /** True when the season best is also the all-time best (a personal best set this season). */
  isPB: boolean;
}

export interface Highlight {
  track: string;
  laps: number;
  fact: string;
}

export interface ProfileData {
  meta: ProfileMeta;
  profile: Profile;
  totals: Totals;
  perTrack: PerTrack[];
  records: RecordRow[];
  /** Optional headline stat for share cards. Omitted by the backend-generated snapshot. */
  highlight?: Highlight;
}

/** One day cell in the activity heatmap. */
export interface HeatmapDay {
  date: Date;
  laps: number;
  /** Intensity bucket 0–4 (0 = no activity). */
  level: 0 | 1 | 2 | 3 | 4;
}

/** A week column: 7 cells, leading/trailing pad cells are `null`. */
export type HeatmapWeek = (HeatmapDay | null)[];

// ─────────────────────────────────────────────────────────────────────────────
// Bundled sample snapshot — one season's aggregates + all-time bests.
// ─────────────────────────────────────────────────────────────────────────────
export const SAMPLE_PROFILE: ProfileData = {
  meta: {
    slug: "marco-rossi",
    season: "2025 Season",
    range: "Jan 1 – May 26, 2025",
    generatedAt: "2025-05-26T21:40:00Z",
    sim: "Assetto Corsa Competizione",
  },

  profile: {
    name: "Marco Rossi",
    slug: "marco-rossi",
    initials: "MR",
    tagline: "Sim racer · GT3 & GT4",
    location: "Milano, IT",
    member_since: "2023",
  },

  totals: {
    laps: 2018,
    distanceKm: 9740,
    hours: 71.4,
    sessions: 142,
    daysActive: 96,
    longestStreak: 14,
    tracks: 6,
    cars: 5,
    topCar: "Ferrari 488 GT3 Evo",
  },

  perTrack: [
    { track: "Spa-Francorchamps", laps: 612, art: "Spa-Francorchamps", accent: "#00d4ff" },
    { track: "Monza", laps: 401, art: "Monza", accent: "#e8212a" },
    { track: "Brands Hatch", laps: 318, art: "Brands Hatch", accent: "#eab308" },
    { track: "Nürburgring", laps: 274, art: "Nurburgring", accent: "#22c55e" },
    { track: "Misano", laps: 233, art: "Misano", accent: "#f97316" },
    { track: "Barcelona", laps: 180, art: "Barcelona", accent: "#a855f7" },
  ],

  records: [
    { track: "Spa-Francorchamps", car: "Ferrari 488 GT3 Evo", season: "2:04.812", allTime: "2:04.101", allTimeWhen: "Nov 2024", isPB: false },
    { track: "Monza", car: "Porsche 991 II GT3 R", season: "1:47.991", allTime: "1:47.991", allTimeWhen: "May 2025", isPB: true },
    { track: "Brands Hatch", car: "McLaren 720S GT3", season: "1:26.881", allTime: "1:26.881", allTimeWhen: "May 2025", isPB: true },
    { track: "Nürburgring", car: "Ferrari 488 GT3 Evo", season: "2:01.554", allTime: "2:00.998", allTimeWhen: "Aug 2024", isPB: false },
    { track: "Misano", car: "Mercedes-AMG GT3 Evo", season: "1:34.881", allTime: "1:34.612", allTimeWhen: "Feb 2025", isPB: false },
    { track: "Barcelona", car: "Ferrari 488 GT3 Evo", season: "1:59.334", allTime: "1:59.334", allTimeWhen: "May 2025", isPB: true },
  ],

  highlight: {
    track: "Spa-Francorchamps",
    laps: 612,
    fact: "That's 27 laps every week.",
  },
};

// ── Track art — simplified circuit outlines (viewBox 0 0 300 200) ─────────────
export const TRACK_ART: Record<string, string> = {
  "Spa-Francorchamps": "M 60 180 L 20 160 L 10 130 L 20 100 L 50 80 L 80 40 L 120 10 L 160 8 L 200 20 L 240 50 L 270 90 L 290 130 L 285 160 L 260 175 L 220 180 L 180 185 L 140 185 L 100 182 Z",
  "Monza": "M 30 160 L 10 120 L 10 80 L 30 50 L 80 20 L 140 10 L 200 10 L 250 30 L 285 60 L 295 100 L 280 140 L 240 165 L 180 175 L 120 175 L 70 170 Z",
  "Brands Hatch": "M 40 60 L 70 35 L 110 30 L 140 45 L 150 80 L 185 95 L 235 90 L 275 110 L 280 145 L 250 170 L 200 175 L 150 168 L 110 175 L 70 165 L 45 135 L 38 95 Z",
  "Nurburgring": "M 50 150 L 25 120 L 20 85 L 45 55 L 90 38 L 135 42 L 160 65 L 200 55 L 245 65 L 278 95 L 285 135 L 262 165 L 215 178 L 165 170 L 130 178 L 90 172 L 62 168 Z",
  "Misano": "M 45 140 L 30 105 L 38 70 L 70 48 L 115 42 L 155 55 L 175 88 L 215 80 L 258 95 L 278 130 L 258 162 L 210 172 L 162 162 L 120 170 L 80 162 Z",
  "Barcelona": "M 55 165 L 28 135 L 22 95 L 45 60 L 88 40 L 135 38 L 178 50 L 205 78 L 248 78 L 280 108 L 282 148 L 252 172 L 205 178 L 158 168 L 112 176 L 78 172 Z",
};

/**
 * Build a GitHub-style activity heatmap for a date range. Returns week columns, each 7 cells
 * (leading/trailing pad cells are null) so it renders Sunday-aligned. Deterministic given `seed` —
 * the bundled demo derives plausible activity; real per-day lap counts will replace this when the
 * snapshot is served from the backend.
 */
export function buildHeatmap(
  start: Date = new Date("2025-01-01T00:00:00Z"),
  end: Date = new Date("2025-05-26T00:00:00Z"),
  seed = 7,
): HeatmapWeek[] {
  const days: HeatmapDay[] = [];
  // simple seeded pseudo-random
  let s = seed;
  const rand = () => {
    s = (s * 9301 + 49297) % 233280;
    return s / 233280;
  };

  for (const d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const dow = d.getDay();
    // More likely to drive on evenings/weekends; clusters into streaks
    const weekendBoost = dow === 0 || dow === 6 ? 0.35 : 0;
    const r = rand();
    let laps = 0;
    if (r + weekendBoost > 0.52) {
      laps = Math.round((r + weekendBoost) * 38);
    }
    let level: HeatmapDay["level"] = 0;
    if (laps > 0) level = 1;
    if (laps >= 12) level = 2;
    if (laps >= 24) level = 3;
    if (laps >= 34) level = 4;
    days.push({ date: new Date(d), laps, level });
  }

  // group into weeks (columns), padding to start on Sunday
  const weeks: HeatmapWeek[] = [];
  if (days.length === 0) return weeks;
  let week: HeatmapWeek = new Array(days[0]!.date.getDay()).fill(null);
  for (const day of days) {
    week.push(day);
    if (week.length === 7) {
      weeks.push(week);
      week = [];
    }
  }
  if (week.length) {
    while (week.length < 7) week.push(null);
    weeks.push(week);
  }
  return weeks;
}

/**
 * Fetches the profile snapshot from the backend, following the `public-profile` HATEOAS link from
 * the bootstrap index (keyed by data mode). In MOCK mode the index advertises the link and the mock
 * handler serves {@link SAMPLE_PROFILE}; in LIVE mode it hits `GET /api/1/public-profile`. The query
 * is disabled (and the page hidden) whenever the backend doesn't advertise the link — i.e. when the
 * public profile is toggled off.
 */
export function useProfileData() {
  const { mode, apiUrl } = useDataMode();
  const href = useFeatures().links["public-profile"];
  const ctx: ApiContext = { mode, apiBase: apiUrl };
  return useQuery({
    queryKey: ["public-profile", mode, apiUrl, href] as const,
    queryFn: () => apiGet<ProfileData>(ctx, href!),
    enabled: !!href,
  });
}

/**
 * Mutation that flips the public profile on/off via the `publicProfileToggle` action link, then
 * invalidates the bootstrap index so `enabledFeatures` + `_links` (and thus the nav/route) refresh.
 */
export function useTogglePublicProfile() {
  const { mode, apiUrl } = useDataMode();
  const toggleHref = useFeatures().links["publicProfileToggle"];
  const queryClient = useQueryClient();
  const ctx: ApiContext = { mode, apiBase: apiUrl };
  return useMutation({
    mutationFn: (enabled: boolean) =>
      apiSend<{ enabled: boolean }>(ctx, "PUT", toggleHref!, { enabled }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["index"] });
    },
  });
}
