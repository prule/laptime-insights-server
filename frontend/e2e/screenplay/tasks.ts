import { Task, Wait } from "@serenity-js/core";
import { Click, isVisible, Navigate } from "@serenity-js/web";
import {
  allTimeRangeButton,
  firstSessionRow,
  screenContainer,
  Screens,
  sessionDetailScreen,
  sidebarLink,
  type Screen,
} from "./screens";

/**
 * Reusable Screenplay Tasks. Specs compose these against an Actor (`actorCalled('Driver')`);
 * no raw Playwright locators appear in test bodies.
 *
 * The app defaults to MOCK data mode (a fresh browser context has no `localStorage` override),
 * so these Tasks drive the in-memory mock with no backend.
 */

/** Open the app at its root (Overview). */
export const OpenTheApp = (): Task =>
  Task.where(
    "#actor opens the app",
    Navigate.to("/"),
    Wait.until(screenContainer(Screens.overview), isVisible()),
  );

/**
 * Widen the global time range to "All". The app defaults to a 1-month window, and the seeded
 * mock sessions predate that, so this is required before the Sessions/Laps lists show any rows.
 */
export const SelectAllTimeRange = (): Task =>
  Task.where("#actor selects the All time range", Click.on(allTimeRangeButton()));

/** Click a sidebar nav item and wait for the target screen to render. */
export const NavigateToScreen = (screen: Screen): Task =>
  Task.where(
    `#actor navigates to the ${screen.name} screen`,
    Click.on(sidebarLink(screen.navHref)),
    Wait.until(screenContainer(screen), isVisible()),
  );

/** From the Sessions list, open the first session's detail page. */
export const OpenTheFirstSession = (): Task =>
  Task.where(
    "#actor opens the first session",
    Wait.until(firstSessionRow(), isVisible()),
    Click.on(firstSessionRow()),
    Wait.until(sessionDetailScreen(), isVisible()),
  );
