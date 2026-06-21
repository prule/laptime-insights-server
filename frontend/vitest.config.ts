/// <reference types="vitest" />
import { defineConfig } from "vitest/config";
import path from "node:path";

/**
 * Default test environment is `node` for the pure-function unit tests (`*.test.ts`). React
 * component / hook tests live in `*.test.tsx` and opt into jsdom per-file via a
 * `// @vitest-environment jsdom` pragma, so the fast node suite stays free of DOM overhead.
 */
export default defineConfig({
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  test: {
    environment: "node",
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
