---
name: vssw:scaffold-idempotent-api
description: >
  Scaffolds a new REST API endpoint. Use this skill when asked to create a POST, PUT, or PATCH route in the backend.
---

# Scaffold Idempotent API Skill

When generating backend API routes, enforce the following REST best practices.

## 1. Idempotency Keys (For Mutations)
All `POST`, `PUT`, and `PATCH` endpoints must require an `Idempotency-Key` header.
- The API must check this key against a cache (e.g., Redis).
- If the key exists, return the cached successful response immediately without processing the request again.

## 2. RFC 7807 Error Handling
If a request fails (validation, unauthorized, not found), the API must return a JSON response formatted according to **RFC 7807 Problem Details**.
```json
{
  "type": "https://example.com/probs/out-of-credit",
  "title": "You do not have enough credit.",
  "status": 403,
  "detail": "Your current balance is 30, but that costs 50.",
  "instance": "/account/12345/msgs/abc"
}
```

## 3. HATEOAS Links
Successful responses should include a `_links` object guiding the client on what they can do next.
```json
{
  "id": "123",
  "status": "pending",
  "_links": {
    "self": { "href": "/orders/123" },
    "cancel": { "href": "/orders/123/cancel", "method": "POST" }
  }
}
```
