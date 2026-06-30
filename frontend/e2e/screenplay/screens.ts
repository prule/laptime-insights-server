import { By, PageElement, PageElements } from "@serenity-js/web";

/**
 * Describes one of the app's main screens: its sidebar nav target, the `data-testid` on its
 * root container, and the title text shown in the Topbar. Drives the navigation Tasks and the
 * screen-content Questions so specs stay declarative.
 *
 * Titles mirror `Topbar.SCREEN_LABELS`. `/live` has no entry there, so it falls back to the app
 * name — and in the default MOCK data mode the Live screen renders its "Switch to Live mode"
 * prompt rather than the live dashboard (the dashboard needs a real backend connection).
 */
export interface Screen {
  name: string;
  /** Sidebar `NavLink` href (matches `FEATURE_CONFIG[...].nav.path`). */
  navHref: string;
  /** `data-testid` on the screen's root container. */
  testId: string;
}

export const Screens = {
  overview: { name: "Overview", navHref: "/", testId: "screen-overview" },
  sessions: { name: "Sessions", navHref: "/sessions", testId: "screen-sessions" },
  laps: { name: "Laps", navHref: "/laps", testId: "screen-laps" },
  compare: { name: "Compare", navHref: "/compare", testId: "screen-compare" },
  live: { name: "Live", navHref: "/live", testId: "screen-live" },
} as const satisfies Record<string, Screen>;

export const AllScreens: Screen[] = Object.values(Screens);

// --- Page elements (the building blocks Questions read) ----------------------------------------

export const sidebarLink = (href: string): PageElement =>
  PageElement.located(By.css(`aside a[href="${href}"]`)).describedAs(`the ${href} sidebar link`);

export const screenContainer = (screen: Screen): PageElement =>
  PageElement.located(By.css(`[data-testid="${screen.testId}"]`)).describedAs(`the ${screen.name} screen`);

/** The Session Detail screen — reached by drilling into a session, so it has no sidebar nav. */
export const sessionDetailScreen = (): PageElement =>
  PageElement.located(By.css('[data-testid="screen-session-detail"]')).describedAs("the Session Detail screen");

export const screenTitle = (): PageElement =>
  PageElement.located(By.css('[data-testid="screen-title"]')).describedAs("the screen title");

export const sessionRows = (): PageElements =>
  PageElements.located(By.css('[data-testid="session-row"]')).describedAs("the session rows");

// Resolve to a single element via `.first()` — the list renders many `session-row`s, and
// `isVisible()`/`Click` need an unambiguous one-element target, not a multi-match locator.
export const firstSessionRow = (): PageElement =>
  sessionRows().first().describedAs("the first session row");

export const lapsTable = (): PageElement =>
  PageElement.located(By.css('[data-testid="laps-table"]')).describedAs("the laps table");

/** The "All" pill in the Topbar time-range selector — clears the default 1-month window. */
export const allTimeRangeButton = (): PageElement =>
  PageElement.located(By.css('[data-testid="time-range-all"]')).describedAs("the All time-range button");

export const lapRows = (): PageElements =>
  PageElements.located(By.css('[data-testid="lap-row"]')).describedAs("the laps table rows");
