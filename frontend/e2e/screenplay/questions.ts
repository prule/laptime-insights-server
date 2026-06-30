import { Question } from "@serenity-js/core";
import { Text } from "@serenity-js/web";
import { lapRows, screenTitle, sessionRows } from "./screens";

/**
 * Questions an Actor can ask about the current screen. Encapsulating these keeps specs free of
 * locator logic — a markup change is a one-file fix here, not a sweep across every test.
 */

/** The title text shown in the Topbar for the current screen. */
export const TheScreenTitle = (): Question<Promise<string>> =>
  Text.of(screenTitle()).describedAs("the screen title");

/** How many session rows are currently listed. */
export const TheNumberOfSessions = (): Question<Promise<number>> =>
  sessionRows().count().describedAs("the number of sessions");

/** How many lap rows the laps table currently shows. */
export const TheNumberOfLaps = (): Question<Promise<number>> =>
  lapRows().count().describedAs("the number of laps");
