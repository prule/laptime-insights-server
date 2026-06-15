## 1. Configuration

- [x] 1.1 Add a single config block (constants `FORM_ACTION` and `EMAIL_ENTRY`) with obvious placeholder values, in the script location chosen for the handler (inline in `landing/index.html` or `landing/src/register-interest.js`).

## 2. Markup

- [x] 2.1 Add a "Register interest" `<section>` to `landing/index.html` with an email `<input type="email" required>`, a submit button, and empty inline status/message elements (aria-live for accessibility).
- [x] 2.2 Style the section with existing Tailwind utility classes consistent with the rest of the page; ensure no horizontal overflow at 375px and correct layout at 1280px.
- [x] 2.3 Confirm the existing primary download/releases CTA is unchanged and the new section reads as a secondary CTA.

## 3. Submit handler

- [x] 3.1 Wire the form on `DOMContentLoaded`: prevent default submit and read the email value.
- [x] 3.2 Validate the email client-side (native + regex guard); block submit and show an inline message for empty/invalid input.
- [x] 3.3 On valid input, POST to `FORM_ACTION` with `mode: "no-cors"` and FormData mapping `EMAIL_ENTRY` -> email.
- [x] 3.4 Show in-progress state, then success ("You're on the list") on resolve; reset/disable the form to prevent duplicate submits.
- [x] 3.5 On a thrown network error, show a retry message and preserve the entered email.
- [x] 3.6 If config is still placeholder, log a clear console warning (defensive, so an unconfigured deploy is obvious).

## 4. Build & static wiring

- [x] 4.1 If the handler is a separate JS file, add it to `landing/scripts/copy-static.mjs` so it is copied into `dist/`.
- [x] 4.2 Run `pnpm build` in `landing/` and confirm `dist/` contains the section and script with no errors.

## 5. Verification

- [x] 5.1 `pnpm preview` (or serve `dist/`): verify validation rejects empty/invalid emails and accepts a valid one.
- [x] 5.2 With placeholder config, verify the no-op path shows the console warning and does not break the page; with a temporary real test form, verify a row lands in the linked Google Sheet and the success state shows.
- [x] 5.3 Verify responsive layout at 375px and 1280px; confirm `app/` and `frontend/` builds are unaffected.

## 6. Documentation

- [x] 6.1 Update `landing/README.md` with how to create the Google Form + linked Sheet, find the `formResponse` URL and `entry.<id>`, and set the config constants.
- [x] 6.2 Note in `docs/technical-debt.md` the deferred items (no spam protection / opaque response can't confirm Google recorded the row) for future hardening.
