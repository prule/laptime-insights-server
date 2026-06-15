## Context

The landing page (`landing/`) is a standalone static site: plain `index.html` + Tailwind
CSS compiled via the Tailwind CLI, with no JS framework, no bundler, and no backend. It
deploys to Cloudflare Pages as static assets. We need to capture visitor emails into a
waiting list without introducing a server, a database, or a build-time secret.

"Add an email to a Google Spreadsheet" is achievable client-side because a Google Form
writes every response into its linked Google Sheet. We can submit to the form's
`formResponse` endpoint directly from the browser. The user has chosen the inline-POST
approach (no redirect) and will supply the form URL / entry id later.

## Goals / Non-Goals

**Goals:**
- Capture a visitor email into a Google Sheet waiting list from the static page.
- Keep the visitor on the page; show success/error feedback inline.
- No backend, no new runtime dependency, no secrets in the repo.
- Make the form URL + entry id trivially configurable later.

**Non-Goals:**
- Server-side validation, deduplication, or storage (the Sheet is the store).
- CAPTCHA / spam protection (can be a later change if abuse appears).
- Double opt-in / confirmation email.
- Analytics on conversion (out of scope).

## Decisions

### Decision: Inline `no-cors` POST to Google Form `formResponse`
On submit, JS sends `fetch(formActionUrl, { method: "POST", mode: "no-cors", body: FormData })`
where the FormData sets `entry.<id> = email`. Google accepts this cross-origin write; the
response is opaque, so a resolved promise (no thrown error) is treated as success.

- **Why:** Zero backend, keeps the visitor on-page, matches the "prefill"/Google-Sheet
  intent. Google Forms reliably accept anonymous cross-origin form posts.
- **Alternatives considered:**
  - *Redirect to a prefilled form URL* — robust but leaves the site; rejected by user.
  - *Embed the form in an `<iframe>`* — no custom JS but ugly styling and awkward prefill; rejected by user.
  - *Cloudflare Pages Function / Worker proxy* — would give a real response and spam
    control, but adds a runtime to a deliberately static site. Deferred unless needed.

### Decision: Configuration in one small JS module / constant block
The form action URL and `entry.<id>` key live as named constants at the top of a single
script (inline in `index.html`, or a `landing/src/register-interest.js` copied to `dist/`
via `copy-static.mjs`). Placeholders are obvious, e.g.
`const FORM_ACTION = "REPLACE_WITH_GOOGLE_FORM_FORMRESPONSE_URL";`
`const EMAIL_ENTRY = "entry.REPLACE_ME";`

- **Why:** Spec requires set-once configuration separate from markup; one location avoids
  hunting through HTML.
- **Alternative:** `data-` attributes on the form element — viable, but a constants block
  reads clearer for a maintainer and keeps validation/feedback logic colocated.

### Decision: Native + lightweight email validation
Use `<input type="email" required>` for browser-native checks, plus a small JS regex
guard before submit so feedback is consistent across browsers and the handler stays in
control of messaging.

### Decision: Plain DOM JS, no dependency
The handler is a few dozen lines of vanilla JS attached on `DOMContentLoaded`. No npm
package, consistent with the site having only `@tailwindcss/cli` as a dev dep.

## Risks / Trade-offs

- **Opaque response means we can't confirm Google actually recorded the row** → Treat
  completion as success (per spec); document the limitation; the linked Sheet is the
  source of truth a maintainer can check. A future Worker proxy could give real status.
- **No spam protection on a public unauthenticated endpoint** → Accept for v1 (low
  traffic, pre-launch). Add honeypot field / Turnstile / Worker proxy later if abused.
- **Google Form entry id changes if the form is recreated** → Centralized config makes the
  fix a one-line edit; documented in `landing/README.md`.
- **Shipping with placeholders would silently no-op** → Make placeholders obviously fake
  and document them so deploy isn't done before configuration; optionally have the handler
  log a clear console warning when still unconfigured.

## Migration Plan

1. Build the feature with placeholder config (this change).
2. Maintainer creates a Google Form with one email question, links it to a Google Sheet
   ("waiting list"), and reads off the `formResponse` URL + `entry.<id>` (via the form's
   prefill link or page source).
3. Paste the real values into the config constants; rebuild (`pnpm build`) and deploy.
4. **Rollback:** the section is self-contained; removing the section markup + script
   reverts cleanly with no effect on the rest of the static page.

## Open Questions

- None blocking. (Spam protection and conversion analytics are intentionally deferred.)
