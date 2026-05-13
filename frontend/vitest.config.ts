/// <reference types="vitest" />
import { defineConfig } from "vitest/config";

/**
 * Pure-function unit tests only — no DOM, no React component rendering yet. Run with
 * `npm test` (CI) or `npm run test:watch` while developing.
 */
export default defineConfig({
  test: {
    environment: "node",
    include: ["src/**/*.test.ts"],
  },
});
