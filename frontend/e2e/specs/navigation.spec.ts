import { Ensure, equals } from "@serenity-js/assertions";
import { describe, it } from "@serenity-js/playwright-test";
import { isVisible } from "@serenity-js/web";
import { AllScreens, screenContainer, Screens } from "../screenplay/screens";
import { TheScreenTitle } from "../screenplay/questions";
import { NavigateToScreen, OpenTheApp } from "../screenplay/tasks";

/**
 * Primary navigation flows, driven through the Screenplay pattern against the app in MOCK mode.
 * Each test asserts the target screen's root container renders — proof the route mounted without
 * a runtime error — rather than scraping incidental markup.
 */
describe("Navigation", () => {
  it("lands on the Overview screen", async ({ actor }) => {
    await actor.attemptsTo(
      OpenTheApp(),
      Ensure.that(screenContainer(Screens.overview), isVisible()),
      Ensure.that(TheScreenTitle(), equals("Overview")),
    );
  });

  AllScreens.forEach((screen) => {
    it(`shows the ${screen.name} screen`, async ({ actor }) => {
      await actor.attemptsTo(
        OpenTheApp(),
        NavigateToScreen(screen),
        Ensure.that(screenContainer(screen), isVisible()),
      );
    });
  });
});
