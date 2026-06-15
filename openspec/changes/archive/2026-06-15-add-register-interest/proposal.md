## Why

The marketing landing page currently only drives visitors to download the self-hosted
app. There is no way to capture interest from visitors who are not ready to download
(or who land before a release is available), so that demand is lost. Capturing emails
into a waiting list lets us gauge interest and reach out when there is something new to
share.

## What Changes

- Add a "Register interest" section to the landing page with an email input and submit button.
- On submit, the email is sent to a Google Form (no-cors POST to its `formResponse`
  endpoint); the form's linked Google Sheet is the waiting list. The visitor never
  leaves the page.
- Show inline success and error states ("You're on the list" / a retry message), with
  basic client-side email validation.
- The Google Form URL and the email entry field ID are stored as clearly-marked
  placeholder constants to be filled in once the form/sheet exist.
- Keep the landing page static — no backend, no new runtime dependency. The only added
  code is a small inline (or single static) JS handler.
- Update landing docs (`landing/README.md`) to describe the integration and how to set
  the form URL / entry ID.

## Capabilities

### New Capabilities
- `register-interest`: Visitor-facing email capture on the landing page that records
  interested users into a Google Sheet waiting list via a Google Form, with inline
  validation and success/error feedback, on a static no-backend site.

### Modified Capabilities
<!-- None. The existing download CTA requirement in marketing-landing-page is unchanged;
     this adds a secondary, independent call-to-action. -->

## Impact

- `landing/index.html`: new "Register interest" section markup and form.
- `landing/src/styles.css` (Tailwind source) if any utility classes need to be present.
- New small JS for the submit handler (inline in `index.html` or a static file copied to
  `dist/` via `landing/scripts/copy-static.mjs`).
- `landing/README.md`: setup notes for the Google Form/Sheet and placeholder values.
- No changes to `app/` or `frontend/`. No new build/runtime dependencies.
- External dependency: a Google Form linked to a Google Sheet (created/owned outside the
  repo); the form URL and entry ID are configuration, not code.
