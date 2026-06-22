## Context

Laptime Insights is self-hosted with no cloud backend, so feedback cannot be collected
server-side without introducing a cloud dependency the architecture deliberately avoids.
The marketing site already solves an equivalent problem with its `register-interest`
capability: a client-only form that POSTs to a Google Form `formResponse` endpoint via a
`no-cors` request, mapping fields to `entry.<id>` ids, with the linked Google Sheet as the
inbox. This change brings the same proven technique into the React dashboard (`frontend/`)
for bug/suggestion/feedback capture.

Relevant current state:
- App shell: `frontend/src/components/layout/{AppShell,Topbar,Sidebar}.tsx`. The Topbar
  already hosts a global control (`TimeRangeSelector`) — the natural slot for a launcher.
- A reusable `Modal` exists under `src/components/ui/`.
- Data mode (MOCK | LIVE) comes from `DataModeProvider`; it is part of every query key.
- The frontend does not yet read `import.meta.env`; this is the first such use.
- App version is available from `frontend/package.json` (`0.1.0`).
- Current screen is available via React Router (`useLocation`), mirroring `Topbar`'s
  existing `SCREEN_LABELS` mapping.

## Goals / Non-Goals

**Goals:**
- Persistent, always-reachable feedback launcher in the dashboard shell.
- Capture type + message (required) + optional email, with auto-attached app version and
  current screen.
- Submit directly to a configurable Google Form, no backend change, user stays in-app.
- Inline in-progress / success / failure states; no real network call in MOCK mode.
- Reuse the register-interest submission technique and conventions.

**Non-Goals:**
- No backend (`app/`) changes, no new REST endpoint, no HATEOAS rel.
- No in-app history/listing of submitted feedback.
- No file/screenshot attachments, no severity/steps fields (kept minimal per scope).
- No server-side feature toggle; gating is config-presence only.

## Decisions

**1. Pure frontend → Google Form, no backend.**
Mirrors `register-interest` and respects the no-cloud, self-hosted model. Alternatives
considered: backend collector POSTing to an external endpoint (adds a cloud dependency and
secret management to a local app) and local-only storage (feedback never reaches the
maintainer). Google Form wins on simplicity and precedent.

**2. Submit via `no-cors` POST to `…/formResponse`, fields → `entry.<id>`.**
Same as register-interest. A `no-cors` response is opaque, so a fetch that resolves
without throwing is treated as success. The submission helper is a small, testable module
(e.g. `src/lib/feedback.ts` or `src/api/feedback.ts`) that builds the `FormData`/body from
a typed payload and the entry-id config.

**3. Launcher lives in the Topbar, not the feature-gated nav.**
Feedback is not a data screen, so it does not belong in `FEATURE_CONFIG`/`enabledFeatures`
and must not be gated on a HATEOAS rel (per the `_links` vs `enabledFeatures` rule). It
renders whenever feedback config is present. Placing it beside `TimeRangeSelector` keeps it
visible on every screen via the shared shell.

**4. Config via Vite build-time env, presence-gated.**
New `VITE_FEEDBACK_FORM_URL` plus entry-id vars (e.g. `VITE_FEEDBACK_ENTRY_TYPE`,
`_MESSAGE`, `_EMAIL`, `_VERSION`, `_SCREEN`). A typed config reader returns `null` when the
URL is unset, and the launcher renders only when config is non-null. This lets an operator
point at their own form and keeps the form hidden in unconfigured builds. Documented via a
committed `.env.example` and `docs/frontend-technical.md`.

**5. App version + current screen auto-attached.**
Version sourced from `package.json` (exposed through Vite `define` as a compile-time
constant, e.g. `__APP_VERSION__`, or imported). Screen derived from `useLocation()` at the
moment the modal opens, reusing the Topbar label semantics so the value is human-readable.

**6. MOCK mode short-circuits the network.**
The submit helper checks the active data mode (`useDataMode`); in MOCK it resolves success
without calling fetch, so the UI is fully exercisable offline and in tests. Consistent with
the mock/live split used everywhere else.

**7. Type modeled as a small enum/union.**
`'bug' | 'suggestion' | 'feedback'` with a default, rendered via the existing
`FilterSelect`/select UI. Sent as the human label or stable key mapped to the configured
type entry id.

## Risks / Trade-offs

- **Opaque `no-cors` response can mask server-side rejection** → Treat resolved fetch as
  success (documented limitation, same as register-interest); failures only surface as
  network errors. Acceptable for low-stakes feedback.
- **Entry-id config drift if the Google Form is edited** → Centralize ids in one config
  module and document them next to the `.env.example`; a wrong id silently drops a field,
  so note this in docs.
- **First use of `import.meta.env` in the frontend** → Add a typed accessor and
  `vite-env.d.ts` declarations so usage is checked; keep all env reads behind that module.
- **Spam/abuse potential** → Out of scope for a self-hosted maintainer tool; Google Form's
  own controls are sufficient. Note as a future option if it becomes a problem.

## Migration Plan

Additive frontend-only change; no data migration. Rollback = revert the frontend commit.
Builds without the env vars set simply omit the launcher, so shipping the code ahead of
configuring the form is safe.

## Open Questions

- Exact Topbar placement/icon for the launcher (icon button vs text) — resolve during
  implementation against existing Topbar styling.
- Whether to also surface the launcher in the mobile/collapsed Sidebar — default to Topbar
  only unless the shell hides the Topbar on small screens.
