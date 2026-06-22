import type { FeedbackConfig } from "../config/feedback";
import type { DataMode } from "../providers/DataModeProvider";

/** The kinds of feedback a user can file. */
export type FeedbackType = "bug" | "suggestion" | "feedback";

export const FEEDBACK_TYPE_LABELS: Record<FeedbackType, string> = {
  bug: "Bug",
  suggestion: "Suggestion",
  feedback: "General feedback",
};

export const DEFAULT_FEEDBACK_TYPE: FeedbackType = "bug";

export interface FeedbackPayload {
  type: FeedbackType;
  message: string;
  /** Optional follow-up email. */
  email?: string;
  /** Auto-attached context. */
  appVersion: string;
  screen: string;
}

export interface FeedbackErrors {
  message?: string;
  email?: string;
}

// Deliberately lenient: block the obviously-broken, accept anything plausible.
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export function isValidEmail(email: string): boolean {
  return EMAIL_RE.test(email);
}

/**
 * Validate the user-entered fields. Returns a map of field -> message; empty
 * object means valid. Message is required; email is optional but must be
 * syntactically valid when provided.
 */
export function validateFeedback(message: string, email: string): FeedbackErrors {
  const errors: FeedbackErrors = {};
  if (!message.trim()) errors.message = "Please enter a message.";
  if (email.trim() && !isValidEmail(email.trim())) errors.email = "Please enter a valid email address.";
  return errors;
}

/**
 * Submit feedback to the configured Google Form.
 *
 * Uses a `no-cors` POST to the form's `formResponse` endpoint, mapping each
 * field to its configured `entry.<id>`. A `no-cors` response is opaque, so a
 * fetch that resolves without throwing is treated as success. In MOCK data mode
 * no request is sent — submission is simulated so the UI works offline.
 */
export async function submitFeedback(
  payload: FeedbackPayload,
  config: FeedbackConfig,
  mode: DataMode,
): Promise<void> {
  if (mode === "mock") return;

  const { entryIds } = config;
  const body = new FormData();
  body.append(entryIds.type, FEEDBACK_TYPE_LABELS[payload.type]);
  body.append(entryIds.message, payload.message.trim());
  if (entryIds.email && payload.email?.trim()) body.append(entryIds.email, payload.email.trim());
  if (entryIds.version) body.append(entryIds.version, payload.appVersion);
  if (entryIds.screen) body.append(entryIds.screen, payload.screen);

  await fetch(config.formUrl, { method: "POST", mode: "no-cors", body });
}
