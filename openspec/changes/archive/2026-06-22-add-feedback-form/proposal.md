## Why

Laptime Insights is self-hosted with no cloud component, so today there is no path for
users to report bugs or suggest improvements without leaving the app and finding the
project repo on their own. We need a low-friction, in-dashboard way to capture feedback
and route it back to the maintainer so the product can actually improve.

## What Changes

- Add a **Feedback** launcher to the dashboard shell (always available, not tied to a
  data feature) that opens a feedback form in a modal.
- The form captures a **type** (bug / suggestion / general feedback), a required free-text
  **message**, an optional **email** for follow-up, and **auto-attached context**
  (app version and current screen) so reports are actionable without extra typing.
- On submit, the form POSTs to a configured **Google Form** `formResponse` endpoint via a
  `no-cors` request, mapping each field to a configured form `entry.<id>` — the same
  integration technique already used by the marketing site's register-interest form. The
  Google Form's linked Google Sheet becomes the feedback inbox.
- The user stays in the dashboard; the form provides inline in-progress / success /
  failure states and guards against duplicate submission.
- Form endpoint and entry-id mappings are supplied via frontend build-time config
  (Vite env), so an operator can point it at their own Google Form. When unconfigured,
  the feedback launcher is hidden.
- In MOCK data mode no real network request is sent; submission is simulated so the UI
  can be exercised offline.

## Capabilities

### New Capabilities
- `feedback-form`: An in-dashboard feedback capture surface (launcher + modal form) that
  validates input, attaches app context, and submits to a configured Google Form,
  reporting inline submission state.

### Modified Capabilities
<!-- None: no existing dashboard capability's requirements change. -->

## Impact

- **frontend/** (the only code that changes):
  - New feedback components under `src/components/` (launcher button + modal form).
  - Wiring of the launcher into the app shell (Topbar/Sidebar).
  - A small config/env addition for the Google Form URL and entry-id mappings
    (mirrors the register-interest config), plus `.env`/sample docs.
  - Vitest coverage for validation, submit mapping, and state transitions.
- **Backend (`app/`)**: no change. Feedback is a pure frontend → Google Form integration,
  consistent with the existing register-interest precedent.
- **Docs**: update `docs/frontend-technical.md` (and user-guide) to describe the feature
  and its config.
- **External dependency**: a Google Form / Sheet owned by the maintainer; no new code
  dependencies.
