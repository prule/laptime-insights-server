import type { ReactElement } from "react";
import { CompareScreen } from "../screens/CompareScreen";
import { LapsScreen } from "../screens/LapsScreen";
import { LiveScreen } from "../screens/LiveScreen";
import { OverviewScreen } from "../screens/OverviewScreen";
import { PublicProfileScreen } from "../screens/PublicProfileScreen";
import { SessionDetailScreen } from "../screens/SessionDetailScreen";
import { SessionsScreen } from "../screens/SessionsScreen";
import type { Feature } from "./feature-types";

/**
 * Frontend mirror of the backend `Feature` enum. Each entry is the single source of truth for one
 * feature's HATEOAS link relation, its sidebar nav, and its router routes. Adding a new feature
 * means appending one entry here; everything else (sidebar, routes, query gating) reads from this
 * registry rather than hard-coding paths.
 *
 * `rel` matches the link relation emitted by `GET /api/1` on the backend — when that link is
 * absent the feature is treated as disabled, its nav + routes are hidden, and its queries are
 * short-circuited (see `FeaturesProvider` + `useFeatureEnabled`).
 *
 * The `Feature` union and `FEATURES` list live in `feature-types.ts` (no React/screen imports)
 * to keep low-level modules — providers and api hooks — out of the screen-component cycle.
 */
export { FEATURES, type Feature } from "./feature-types";

export interface FeatureRoute {
  /** React Router path relative to the AppShell root. */
  path: string;
  element: ReactElement;
}

export interface FeatureNav {
  label: string;
  icon: string;
  /** Sidebar target — the primary path users land on when they click the nav item. */
  path: string;
  /** Pass to `NavLink`'s `end` prop so root paths don't match all sub-routes. */
  end?: boolean;
}

export interface FeatureConfig {
  id: Feature;
  /** HATEOAS rel from `GET /api/1` `_links`. */
  rel: Feature;
  nav: FeatureNav;
  routes: FeatureRoute[];
}

export const FEATURE_CONFIG: Record<Feature, FeatureConfig> = {
  overview: {
    id: "overview",
    rel: "overview",
    nav: { label: "Overview", icon: "◈", path: "/", end: true },
    routes: [{ path: "/", element: <OverviewScreen /> }],
  },
  sessions: {
    id: "sessions",
    rel: "sessions",
    nav: { label: "Sessions", icon: "◫", path: "/sessions" },
    routes: [
      { path: "/sessions", element: <SessionsScreen /> },
      { path: "/sessions/:uid", element: <SessionDetailScreen /> },
    ],
  },
  laps: {
    id: "laps",
    rel: "laps",
    nav: { label: "Laps", icon: "◷", path: "/laps" },
    routes: [{ path: "/laps", element: <LapsScreen /> }],
  },
  compare: {
    id: "compare",
    rel: "compare",
    nav: { label: "Compare", icon: "◪", path: "/compare" },
    routes: [{ path: "/compare", element: <CompareScreen /> }],
  },
  live: {
    id: "live",
    rel: "live",
    nav: { label: "Live", icon: "◉", path: "/live" },
    routes: [{ path: "/live", element: <LiveScreen /> }],
  },
  "public-profile": {
    id: "public-profile",
    rel: "public-profile",
    nav: { label: "Public Profile", icon: "🪪", path: "/profile" },
    routes: [{ path: "/profile", element: <PublicProfileScreen /> }],
  },
};
