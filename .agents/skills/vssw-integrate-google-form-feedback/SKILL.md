---
name: vssw:integrate-google-form-feedback
description: >
  Implements a user feedback mechanism using Google Forms instead of a custom database.
  Use this skill whenever asked to add a feedback feature, support form, or survey to the app.
---

# Integrate Google Form Feedback Skill

In accordance with our "Build vs. Buy" engineering principle, we do not build custom database tables or backend APIs for collecting simple user feedback. We use Google Forms.

When asked to implement a feedback mechanism, enforce the following pattern:

## 1. Avoid Over-Engineering
- Do not generate database migrations, backend routes, or data models for feedback.
- Inform the user that you are implementing a lightweight Google Forms integration to save development time.

## 2. Generate the Native React UI
- The user must **never** see the actual Google Form UI (do not use iframes or new tabs).
- Build a native React form component (e.g., inside a Modal/Dialog) matching the application's design system and styling (e.g., Tailwind CSS).
- Include native inputs for the feedback message, email, etc.

## 3. Submit via `no-cors` POST
- When the React form is submitted, construct a `FormData` object.
- Map the React form state and any application context (e.g., `userId`, `currentUrl`, `appVersion`) to the specific Google Form `entry.<id>` fields.
- Submit the data to the Google Form's `formResponse` URL using a `fetch` POST request with `mode: "no-cors"`.
- Because `no-cors` returns an opaque response, treat any fetch that resolves without throwing a network error as a successful submission.

Provide a code example showing this submission pattern:
```typescript
export async function submitFeedback(message: string, userId: string) {
  const formResponseUrl = "https://docs.google.com/forms/d/e/.../formResponse";
  
  const body = new FormData();
  body.append("entry.123456", message);
  body.append("entry.654321", userId);
  body.append("entry.987654", window.location.pathname);

  // no-cors prevents the browser from blocking the cross-origin POST.
  // The response is opaque, so if it doesn't throw, we assume success.
  await fetch(formResponseUrl, { method: "POST", mode: "no-cors", body });
}
```
