---
name: vssw:generate-db-migration
description: >
  Generates a zero-downtime Flyway SQL migration script. 
  Use this skill when asked to create, modify, or drop database tables/columns.
---

# Generate DB Migration Skill

When asked to generate a database migration, you must strictly follow these rules from the engineering playbook.

## 1. Naming Conventions
- Table names must be `snake_case` and plural (e.g., `user_accounts`, `orders`).
- Column names must be `snake_case` (e.g., `first_name`, `created_at`).
- Primary keys must be named `id` and be of type `UUID`.

## 2. Zero-Downtime Rule (Expand and Contract)
We never execute destructive operations (`RENAME COLUMN`, `DROP COLUMN`, `ALTER TYPE`) in a single migration, as this causes downtime while the application deploys.
Instead, use the **Expand and Contract** pattern. 

If the user asks for a breaking change:
1. **Expand:** Generate a migration that *adds* the new column alongside the old one. 
2. **Explain:** Tell the user that they must now update the application code to write to *both* columns and read from the new one.
3. **Contract (Later):** Tell the user that a future migration will drop the old column once the code is fully deployed.

## 3. Standard Columns
Every new table must include:
- `id UUID PRIMARY KEY DEFAULT gen_random_uuid()`
- `created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()`
- `updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()`

## 4. Output
Output the raw SQL script using a standard Flyway naming convention (e.g., `V<timestamp>__add_user_accounts.sql`).
