---
name: vssw:scaffold-hateoas-feature-flag
description: >
  Implements a new feature flag using the HATEOAS pattern. Use this skill when the user asks to hide a new feature behind a toggle.
---

# Scaffold HATEOAS Feature Flag Skill

We do not evaluate feature flags on the frontend. The backend is the sole source of truth.

## 1. Backend Implementation
- The backend evaluates the feature flag (e.g., `isNewCheckoutEnabled(userId)`).
- If the flag is `true`, the backend injects a specific link into the `_links` object of the relevant REST resource.
- Example: If the flag is on, add `"checkout_v2": { "href": "/checkout/v2" }`.

## 2. Frontend Implementation
- The frontend React component must NOT evaluate flags.
- Instead, the component simply checks if the link exists in the `_links` object.
- If the link exists, it renders the button/UI element and uses the `href` provided by the backend to perform the action.

## 3. Output
When using this skill, output both the backend link-injection code block and the frontend conditional-rendering React component.
