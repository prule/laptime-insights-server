import type { PlaywrightTestConfig } from "@serenity-js/playwright-test";
import { devices } from "@playwright/test";

/**
 * Playwright + Serenity/JS (Screenplay) E2E config.
 *
 * Data mode: the app defaults to MOCK (see `src/providers/DataModeProvider` — `DEFAULTS.mode`,
 * overridable only via `localStorage["lti.dataMode"]`). A fresh Playwright browser context has
 * empty localStorage, so every test runs against the in-memory mock with no backend or ACC
 * telemetry — fully deterministic. No `webServer.env` is required.
 *
 * Server: tests run against the production bundle served by `vite preview` for CI realism.
 * `reuseExistingServer` lets local devs pre-start `pnpm preview` to skip the rebuild.
 */
const PORT = 4173;
const baseURL = `http://localhost:${PORT}`;

const config: PlaywrightTestConfig = {
  testDir: "./e2e/specs",
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 1 : 0,

  reporter: [
    ["list"],
    // Serenity/JS event glue + artifact archiving (screenshots/traces under target/site/serenity).
    [
      "@serenity-js/playwright-test",
      {
        crew: [["@serenity-js/core:ArtifactArchiver", { outputDirectory: "target/site/serenity" }]],
      },
    ],
    ["html", { open: "never" }],
  ],

  use: {
    baseURL,
    // Default Screenplay Actor; tests use `actorCalled('Driver')`.
    defaultActorName: "Driver",
    crew: [["@serenity-js/web:Photographer", { strategy: "TakePhotosOfFailures" }]],
    trace: "on-first-retry",
  },

  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],

  webServer: {
    command: `pnpm build && pnpm preview --port ${PORT} --strictPort`,
    url: baseURL,
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
};

export default config;
