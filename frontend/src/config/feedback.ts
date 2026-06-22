/**
 * Feedback → Google Form configuration, read from Vite build-time env.
 *
 * Returns `null` when the feature is not configured (no form URL or missing the
 * required type/message entry ids) so the UI can presence-gate the launcher.
 * This is intentionally NOT a HATEOAS/`enabledFeatures` toggle — feedback is a
 * frontend-only integration with no backend rel.
 */
export interface FeedbackConfig {
  /** Google Form action URL ending in `/formResponse`. */
  formUrl: string;
  /** `entry.<id>` field names; type and message are required, the rest optional. */
  entryIds: {
    type: string;
    message: string;
    email?: string;
    version?: string;
    screen?: string;
  };
}

export function readFeedbackConfig(): FeedbackConfig | null {
  const formUrl = import.meta.env.VITE_FEEDBACK_FORM_URL?.trim();
  const type = import.meta.env.VITE_FEEDBACK_ENTRY_TYPE?.trim();
  const message = import.meta.env.VITE_FEEDBACK_ENTRY_MESSAGE?.trim();

  if (!formUrl || !type || !message) return null;

  return {
    formUrl,
    entryIds: {
      type,
      message,
      email: import.meta.env.VITE_FEEDBACK_ENTRY_EMAIL?.trim() || undefined,
      version: import.meta.env.VITE_FEEDBACK_ENTRY_VERSION?.trim() || undefined,
      screen: import.meta.env.VITE_FEEDBACK_ENTRY_SCREEN?.trim() || undefined,
    },
  };
}
