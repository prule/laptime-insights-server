---
name: vssw:scaffold-ktor-search-criteria
description: >
  Scaffolds a search criteria data class and its Exposed Query translation logic.
  Use this skill when asked to build search, filtering, or pagination for an entity.
---

# Scaffold Ktor Search Criteria Skill

When building search capabilities, enforce the following patterns:

## 1. The Domain Model
Create a `SearchCriteria` data class containing nullable fields representing the possible filters.
```kotlin
data class UserSearchCriteria(
  val id: Uid? = null,
  val status: Status? = null,
  val from: Instant? = null
)
```

## 2. The Exposed Translation (`toQuery`)
Create an extension function `SearchCriteria.toQuery(): Query` in the same file as the Repository (or an adjacent data access file).
This function dynamically builds the `andWhere` clauses based on which fields in the criteria are not null.

```kotlin
fun UserSearchCriteria.toQuery(): Query {
  val query = UserTable.selectAll()
  id?.let { query.andWhere { UserTable.id eq it.value } }
  status?.let { query.andWhere { UserTable.status eq it.name } }
  from?.let { query.andWhere { UserTable.createdAt greaterEq it } }
  return query
}
```

## 3. Dynamic Joins
If the criteria contains facets that require querying an adjacent table, dynamically apply a `join` to the base `Query` only when those facets are present.
