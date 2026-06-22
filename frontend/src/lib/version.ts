/**
 * App version, injected at build time from package.json via Vite `define`
 * (`__APP_VERSION__`). Falls back to a dev sentinel in environments without the
 * define (e.g. the Vitest node runner), so importing this never throws.
 */
export const APP_VERSION: string =
  typeof __APP_VERSION__ !== "undefined" ? __APP_VERSION__ : "0.0.0-dev";
