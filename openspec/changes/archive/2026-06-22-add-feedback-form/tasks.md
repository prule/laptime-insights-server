## 1. Config & env plumbing

- [x] 1.1 Add `VITE_FEEDBACK_FORM_URL` and entry-id vars (`VITE_FEEDBACK_ENTRY_TYPE`, `_MESSAGE`, `_EMAIL`, `_VERSION`, `_SCREEN`) to a committed `frontend/.env.example`
- [x] 1.2 Declare the vars in `frontend/src/vite-env.d.ts` (ImportMetaEnv) for type safety
- [x] 1.3 Add a typed config reader (e.g. `src/config/feedback.ts`) returning a `FeedbackConfig | null` (null when URL unset)
- [x] 1.4 Expose app version as a compile-time constant via Vite `define` (`__APP_VERSION__` from `package.json`) and declare its type

## 2. Submission helper

- [x] 2.1 Add a typed `FeedbackPayload` (type, message, email?, appVersion, screen) and a submit helper (`src/api/feedback.ts` or `src/lib/feedback.ts`)
- [x] 2.2 Build the `formResponse` `no-cors` POST mapping each field to its configured `entry.<id>`; treat a non-throwing fetch as success
- [x] 2.3 Short-circuit in MOCK data mode (resolve success without a network call)

## 3. Feedback form UI

- [x] 3.1 Create the feedback modal form component (type selector with default, required message textarea, optional email) using the existing `Modal` and select UI
- [x] 3.2 Implement client-side validation (non-empty message; valid email when provided) with inline messages
- [x] 3.3 Wire in-progress / success / failure states; disable submit while sending and after success; allow retry on failure preserving the message
- [x] 3.4 Auto-attach app version and current screen (from `useLocation`) into the payload on submit

## 4. Launcher & shell wiring

- [x] 4.1 Add a Feedback launcher control to `Topbar.tsx` (beside `TimeRangeSelector`) that opens the modal
- [x] 4.2 Render the launcher only when feedback config is present (presence-gated, not via `enabledFeatures`)

## 5. Tests

- [x] 5.1 Unit-test the submit helper: entry-id mapping, success-on-resolve, MOCK short-circuit
- [x] 5.2 Unit-test validation (empty message rejected, invalid email rejected, valid input accepted)
- [x] 5.3 Component-test the form: state transitions (idle → submitting → success/failure) and context attachment
- [x] 5.4 Test launcher visibility: hidden when unconfigured, visible when configured

## 6. Docs & verification

- [x] 6.1 Document the feature and its env config in `docs/frontend-technical.md` (and a note in `docs/user-guide.md`)
- [x] 6.2 Run `pnpm lint`, `pnpm typecheck`, `pnpm test`, `pnpm build` and confirm green
- [x] 6.3 Log any cleanup follow-ups in `docs/technical-debt.md` if discovered
