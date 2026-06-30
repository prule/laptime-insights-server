---
name: vssw:integrate-google-form-register-interest
description: >
  Implements a "register interest" or "waitlist" mechanism using Google Forms instead of a custom database.
  Use this skill whenever asked to add an email capture form, waitlist, or newsletter signup.
---

# Integrate Google Form Register Interest Skill

In accordance with our "Build vs. Buy" engineering principle, we do not build custom database tables or backend APIs for collecting simple "register interest" emails. We use Google Forms.

When asked to implement an email capture or waitlist mechanism, enforce the following pattern:

## 1. Avoid Over-Engineering
- Do not generate database migrations, backend routes, or data models for waitlists.
- Inform the user that you are implementing a lightweight Google Forms integration to save development time.

## 2. Generate the Native React UI
- The user must **never** see the actual Google Form UI.
- Build a native React form component (e.g., a simple input field and a "Notify Me" button) matching the application's design system and styling (e.g., Tailwind CSS).
- Include native validation for the email address.

## 3. Submit via `no-cors` POST
- When the React form is submitted, construct a `FormData` object mapping the entered email to the specific Google Form `entry.<id>` field.
- Submit the data to the Google Form's `formResponse` URL using a `fetch` POST request with `mode: "no-cors"`.
- Because `no-cors` returns an opaque response, treat any fetch that resolves without throwing a network error as a successful submission.

Provide a code example showing this submission pattern:
```typescript
export async function submitInterest(email: string) {
  const formResponseUrl = "https://docs.google.com/forms/d/e/.../formResponse";
  
  const body = new FormData();
  body.append("entry.123456", email);

  // no-cors prevents the browser from blocking the cross-origin POST.
  // The response is opaque, so if it doesn't throw, we assume success.
  await fetch(formResponseUrl, { method: "POST", mode: "no-cors", body });
}
```
