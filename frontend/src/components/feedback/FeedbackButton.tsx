import { useState } from "react";
import { useLocation } from "react-router-dom";
import { Modal } from "../ui/Modal";
import { useDataMode } from "../../providers/DataModeProvider";
import { readFeedbackConfig, type FeedbackConfig } from "../../config/feedback";
import { APP_VERSION } from "../../lib/version";
import {
  DEFAULT_FEEDBACK_TYPE,
  FEEDBACK_TYPE_LABELS,
  submitFeedback,
  validateFeedback,
  type FeedbackErrors,
  type FeedbackType,
} from "../../api/feedback";

type SubmitState = "idle" | "submitting" | "success" | "error";

const TYPE_ORDER: FeedbackType[] = ["bug", "suggestion", "feedback"];

/**
 * Persistent Feedback launcher + modal form. Renders nothing unless a Google
 * Form is configured (presence-gated — NOT a HATEOAS `enabledFeatures` toggle).
 */
export function FeedbackButton() {
  const config = readFeedbackConfig();
  if (!config) return null;
  return <FeedbackButtonInner config={config} />;
}

function FeedbackButtonInner({ config }: { config: FeedbackConfig }) {
  const { mode } = useDataMode();
  const location = useLocation();

  const [open, setOpen] = useState(false);
  const [type, setType] = useState<FeedbackType>(DEFAULT_FEEDBACK_TYPE);
  const [message, setMessage] = useState("");
  const [email, setEmail] = useState("");
  const [errors, setErrors] = useState<FeedbackErrors>({});
  const [state, setState] = useState<SubmitState>("idle");

  function reset() {
    setType(DEFAULT_FEEDBACK_TYPE);
    setMessage("");
    setEmail("");
    setErrors({});
    setState("idle");
  }

  function close() {
    setOpen(false);
    // Reset only once the success state has been acknowledged; otherwise keep
    // the user's draft if they reopen after an error.
    if (state === "success") reset();
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    const found = validateFeedback(message, email);
    setErrors(found);
    if (found.message || found.email) return;

    setState("submitting");
    try {
      await submitFeedback(
        { type, message, email: email.trim() || undefined, appVersion: APP_VERSION, screen: location.pathname },
        config,
        mode,
      );
      setState("success");
    } catch {
      setState("error");
    }
  }

  const submitting = state === "submitting";

  return (
    <>
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="rounded border border-border bg-surface px-3 py-1.5 font-sans text-[13px] text-text outline-none hover:border-cyan/40 focus:border-cyan/40"
      >
        Feedback
      </button>

      <Modal open={open} onClose={close} title="Send feedback" maxWidthClass="max-w-lg">
        {state === "success" ? (
          <div className="flex flex-col gap-4">
            <p className="font-sans text-[13px] text-text">
              Thanks — your feedback was sent. It helps improve LapTime Insights.
            </p>
            <div className="flex justify-end">
              <button
                type="button"
                onClick={close}
                className="rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text hover:border-cyan/40"
              >
                Done
              </button>
            </div>
          </div>
        ) : (
          <form className="flex flex-col gap-4" onSubmit={handleSubmit} noValidate>
            <label className="flex flex-col gap-1">
              <span className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">Type</span>
              <select
                value={type}
                onChange={(e) => setType(e.target.value as FeedbackType)}
                disabled={submitting}
                className="rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none focus:border-cyan/40"
              >
                {TYPE_ORDER.map((t) => (
                  <option key={t} value={t}>
                    {FEEDBACK_TYPE_LABELS[t]}
                  </option>
                ))}
              </select>
            </label>

            <label className="flex flex-col gap-1">
              <span className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">Message</span>
              <textarea
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                disabled={submitting}
                rows={5}
                placeholder="Describe the bug or suggestion…"
                aria-invalid={errors.message ? true : undefined}
                className="resize-y rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none focus:border-cyan/40"
              />
              {errors.message && <span className="font-sans text-[12px] text-red-400">{errors.message}</span>}
            </label>

            <label className="flex flex-col gap-1">
              <span className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
                Email (optional)
              </span>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={submitting}
                placeholder="you@example.com"
                aria-invalid={errors.email ? true : undefined}
                className="rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none focus:border-cyan/40"
              />
              {errors.email && <span className="font-sans text-[12px] text-red-400">{errors.email}</span>}
            </label>

            {state === "error" && (
              <p className="font-sans text-[12px] text-red-400">
                Couldn’t send your feedback. Check your connection and try again.
              </p>
            )}

            <div className="flex justify-end gap-2">
              <button
                type="button"
                onClick={close}
                disabled={submitting}
                className="rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text-muted hover:text-text"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submitting}
                className="rounded border border-cyan/40 bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none hover:border-cyan/60 disabled:opacity-50"
              >
                {submitting ? "Sending…" : "Send feedback"}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </>
  );
}
