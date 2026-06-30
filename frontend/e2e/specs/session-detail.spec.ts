import { Ensure, isGreaterThan } from "@serenity-js/assertions";
import { describe, it } from "@serenity-js/playwright-test";
import { isVisible, Scroll } from "@serenity-js/web";
import { lapsTable, sessionDetailScreen, Screens } from "../screenplay/screens";
import { TheNumberOfLaps } from "../screenplay/questions";
import {
  NavigateToScreen,
  OpenTheApp,
  OpenTheFirstSession,
  SelectAllTimeRange,
} from "../screenplay/tasks";

/**
 * Drill-down flow: Sessions list -> a session's detail page -> its laps table.
 * Runs in MOCK mode, where every seeded session has laps.
 */
describe("Session detail", () => {
  it("opens a session and shows its laps table", async ({ actor }) => {
    await actor.attemptsTo(
      OpenTheApp(),
      NavigateToScreen(Screens.sessions),
      SelectAllTimeRange(),
      OpenTheFirstSession(),
      Ensure.that(sessionDetailScreen(), isVisible()),
      // The laps table sits below the fold; bring it into view before asserting visibility.
      Scroll.to(lapsTable()),
      Ensure.that(lapsTable(), isVisible()),
      Ensure.that(TheNumberOfLaps(), isGreaterThan(0)),
    );
  });
});
