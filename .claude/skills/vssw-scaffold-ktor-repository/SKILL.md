---
name: vssw:scaffold-ktor-repository
description: >
  Scaffolds a Ktor Hexagonal Architecture repository using JetBrains Exposed. 
  Use this skill when asked to create a data access layer / repository for an entity.
---

# Scaffold Ktor Repository Skill

When scaffolding a Kotlin repository using JetBrains Exposed, enforce these patterns:

## 1. Class Signature
- Implement standard interfaces (e.g., `SearchRepository<Entity, Criteria>`, `FindByIdRepository`).
- Pass in any required mappers via constructor injection.

## 2. Exposed Usage
- Use the Exposed DAO API for single-record reads and writes (e.g., `Entity.findById(id)`, `Entity.new { ... }`, `Entity.findByIdAndUpdate { ... }`).
- Use the Exposed DSL for complex queries and pagination.
- **Never** manage database transactions (`transaction { ... }`) inside the repository. The transaction boundary must be managed higher up (e.g., by the Application Service).

## 3. Delegation to Search Criteria
- When implementing a `search` or `searchForOne` function, delegate the `WHERE` clause building to the Search Criteria object using a `criteria.toQuery()` extension function.
- Chain the resulting `Query` with `.paginate(pageRequest, sort)` to handle pagination at the database level.
