---
name: vssw:scaffold-ktor-controller
description: >
  Scaffolds a Ktor REST Controller handling routing, OpenAPI documentation, and HATEOAS.
  Use this skill when asked to create a new API route or endpoint.
---

# Scaffold Ktor Controller Skill

When building API routes in Ktor, enforce the following patterns:

## 1. Class Structure
- Define a class taking the `Application` and any required Application Services (Use Cases) via the primary constructor.
- Register routes inside the `init { application.routing { ... } }` block.

## 2. Type-Safe Routing
- Use Ktor's Type-Safe Routing (Resources). 
- Example: `@Resource("/api/1/users") class UsersRoute` -> `get<UsersRoute> { ... }`.

## 3. OpenAPI Documentation
- Immediately following the route definition block, use the `.describe { ... }` extension to document the endpoint inline.
- Include `summary`, `description` (with Markdown), and `responses` blocks.

## 4. HATEOAS Linking
- Use a dedicated `LinkFactory` to append `_links` to the response DTO. 
- Ensure that the core data returned is purely RESTful, mapping internal Domain Models to serializable DTOs before responding.
