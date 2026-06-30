/**
 * Plain types for the feature toggle system. Kept in its own file (without React/JSX or screen
 * imports) so it can be pulled into low-level modules — `FeaturesProvider`, `api/queries.ts` —
 * without creating an import cycle through `config/features.tsx` (which references the screen
 * components).
 */

export type Feature = "overview" | "sessions" | "laps" | "compare" | "live" | "public-profile";

export const FEATURES: readonly Feature[] = [
  "overview",
  "sessions",
  "laps",
  "compare",
  "live",
  "public-profile",
];
