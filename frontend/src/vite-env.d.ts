/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** Google Form `formResponse` endpoint for feedback submissions. */
  readonly VITE_FEEDBACK_FORM_URL?: string;
  /** `entry.<id>` field name for the feedback type question (required). */
  readonly VITE_FEEDBACK_ENTRY_TYPE?: string;
  /** `entry.<id>` field name for the message question (required). */
  readonly VITE_FEEDBACK_ENTRY_MESSAGE?: string;
  /** `entry.<id>` for the optional follow-up email question. */
  readonly VITE_FEEDBACK_ENTRY_EMAIL?: string;
  /** `entry.<id>` for the auto-attached app version. */
  readonly VITE_FEEDBACK_ENTRY_VERSION?: string;
  /** `entry.<id>` for the auto-attached current screen. */
  readonly VITE_FEEDBACK_ENTRY_SCREEN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

/** App version injected at build time from package.json via Vite `define`. */
declare const __APP_VERSION__: string;
