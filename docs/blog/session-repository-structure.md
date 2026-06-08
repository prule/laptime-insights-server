# The Repository That Doesn't Grow: Criteria over Finder Methods

`SessionRepository` in the Laptime Insights server is ~130 lines and exposes seven public methods. It has served every read path the app needs — listing, single lookup, filter discovery, time-bucketed aggregation, and the writes — without growing a new finder method for each new screen. This post is a tour of how it's structured and why the method surface stays small.

The short version: the repository takes a `SessionSearchCriteria`, not a list of `findBy*` arguments. New search dimensions extend the criteria; new search *semantics* replace it. The repository itself only knows about the criteria interface, so it doesn't grow.

---

## The complete public surface

Here is every public method on the repository, after stripping bodies:

```kotlin
class SessionRepository(private val mapper: SessionMapper) :
  FindByIdRepository<SessionEntity, Long>,
  FindByCriteriaRepository<SessionEntity>,
  SearchRepository<SessionEntity, SessionSearchCriteria> {

  override fun findOneOrNull(id: Long): SessionEntity? = …
  fun create(session: Session): SessionEntity = …
  override fun searchForOne(criteria: SessionSearchCriteria, sort: Sort): SessionEntity? = …
  override fun search(criteria: SessionSearchCriteria, pageRequest: PageRequest, sort: Sort): Page<SessionEntity> = …
  fun update(session: Session): SessionEntity = …
  fun options(criteria: SessionSearchCriteria): SessionOptions = …
  fun aggregate(criteria: SessionSearchCriteria, groupBy: SessionAggregateGroupBy): List<SessionAggregateBucket> = …
}
```

Seven methods. Three are CRUD (`findOneOrNull`, `create`, `update`). Two are search variants over the same criteria type (`searchForOne` and `search`). The remaining two — `options` and `aggregate` — read the same filtered set as a different shape: distinct facet values, and time-bucketed counts.

What's striking is what isn't there. There is no `findByCar`, `findByCarAndTrack`, `findByCarAndTrackAndSimulator`, `findRecentByTrack`, `findBetweenDates`, `findByDateRangeAndSimulator`. Every one of those is expressible as a different `SessionSearchCriteria` and reaches the same `search()` method.

---

## Criteria: one input shape for every read

`SessionSearchCriteria` is a flat data class of nullable fields. `null` means "don't constrain this dimension". Fields combine with logical AND:

```kotlin
// application/domain/model/SessionSearchCriteria.kt
data class SessionSearchCriteria(
  val id: SessionId? = null,
  val uid: Uid? = null,
  val car: Car? = null,
  val track: Track? = null,
  val simulator: Simulator? = null,
  val from: Instant? = null,
  val to: Instant? = null,
) : SearchCriteria { companion object }
```

The translation to SQL lives next to the repository — a single private extension that any read on the repository can call:

```kotlin
// adapter/out/persistence/session/SessionRepository.kt
fun SessionSearchCriteria.toQuery(): Query {
  val query = SessionTable.selectAll()

  id?.let         { query.andWhere { SessionTable.id        eq it.value } }
  uid?.let        { query.andWhere { SessionTable.uid       eq it.value } }
  car?.let        { query.andWhere { SessionTable.car       eq it.value } }
  track?.let      { query.andWhere { SessionTable.track     eq it.value } }
  simulator?.let  { query.andWhere { SessionTable.simulator eq it.name  } }
  from?.let       { query.andWhere { SessionTable.startedAt greaterEq it } }
  to?.let         { query.andWhere { SessionTable.startedAt lessEq    it } }

  return query
}
```

This is the only place in the repository that knows how a criteria field becomes a predicate. `search()`, `searchForOne()`, `options()` and `aggregate()` all start from `criteria.toQuery()` — they only differ in what they do with the result.

### Adding a new filter

The pattern for extending the search dimension is mechanical:

1. Add a nullable field to `SessionSearchCriteria`.
2. Add one `andWhere` clause in `toQuery()`.
3. (Optional) parse the new query param in `SessionSearchCriteria.fromParameters`.

That's it. No new repository methods. No new ports. The use case, persistence adapter, and frontend's `useSessions` hook don't know anything happened. Search by `sessionType`, search by `playerCarId`, search by a UID prefix — none of those need a fresh `findBy…` method.

### Adding a new search *semantic*

If a query can't be expressed by AND-ing predicates over the current schema — say a "best session per track" view, or a free-text fuzzy match — the answer isn't to add a method. It's to introduce a different criteria type (`BestSessionPerTrackCriteria`, `SessionFulltextCriteria`) with its own `toQuery()`, or to keep the same criteria but resolve it via a different base query.

The lap side of the codebase already does the latter. `LapRepository` has a `resolvedQuery()` that swaps base queries depending on a flag on the criteria:

```kotlin
// adapter/out/persistence/lap/LapRepository.kt
private fun resolvedQuery(criteria: LapSearchCriteria): Query =
  if (criteria.allTimeBest?.value == true) bestPerTrackQuery(criteria) else criteria.toQuery()
```

`bestPerTrackQuery` builds a much more involved query — a `ROW_NUMBER() OVER (PARTITION BY track ORDER BY lap_time, id)` window, ranked subquery, `IN (subquery)` join back to `LapTable` — but the result is still a normal `Query` that `paginate(...)` can sort and slice. The complexity stays inside one private method. `LapRepository.search()` still looks the same as `SessionRepository.search()`.

This is the lever the structure gives you: complexity goes into a *new function* with a clear name, not a new repository method that the use case has to discover and call.

---

## Two search variants from one criteria

There are two search methods on the repository because the upstream callers actually want two answers:

```kotlin
override fun searchForOne(criteria: SessionSearchCriteria, sort: Sort): SessionEntity? {
  return criteria.toQuery().firstOrNull(sort, SessionEntity.sortableFields) {
    SessionEntity.wrapRow(it)
  }
}

override fun search(
  criteria: SessionSearchCriteria,
  pageRequest: PageRequest,
  sort: Sort,
): Page<SessionEntity> {
  return criteria.toQuery().paginate(pageRequest, sort, SessionEntity.sortableFields) {
    SessionEntity.wrapRow(it)
  }
}
```

`searchForOne` is the natural answer to "find the most recent session for this car" or "find the session that holds the current lap record on track X" — caller passes the criteria narrow enough to identify a single row, plus the sort that defines "which one". `search` is the answer to "give me a page of matching sessions". Same predicates, different framing.

Both methods delegate the orderBy/limit/offset details to small extension functions on `Query` shared across all repositories (`paginate`, `firstOrNull` in `utils/.../exposed/QueryExtension.kt`). The repository never writes `orderBy(...).limit(...).offset(...)` itself.

---

## `options()` — facets from the same WHERE

`options()` answers "given the current filter selections, what are the distinct values you could pick for cars/tracks/simulators, and what's the available date range?". The frontend uses it to drive cascading filter dropdowns: pick a track and only the cars actually raced at that track show up.

The implementation reuses `toQuery()` and mutates the *projection* rather than rebuilding the filter:

```kotlin
fun options(criteria: SessionSearchCriteria): SessionOptions {
  val query = criteria.toQuery()

  val cars =
    query.copy()
      .adjustSelect { select(SessionTable.car) }
      .withDistinct()
      .mapNotNull { it[SessionTable.car]?.let { Car(it) } }

  val tracks =
    query.copy()
      .adjustSelect { select(SessionTable.track) }
      .withDistinct()
      .mapNotNull { it[SessionTable.track]?.let { Track(it) } }

  val simulators =
    query.copy()
      .adjustSelect { select(SessionTable.simulator) }
      .withDistinct()
      .map { Simulator.valueOf(it[SessionTable.simulator]) }

  val minStartedAt = SessionTable.startedAt.min()
  val maxStartedAt = SessionTable.startedAt.max()
  val rangeRow =
    query.copy().adjustSelect { select(minStartedAt, maxStartedAt) }.toList().firstOrNull()

  return SessionOptions(
    cars = cars, tracks = tracks, simulators = simulators,
    from = rangeRow?.get(minStartedAt),
    to = rangeRow?.get(maxStartedAt),
  )
}
```

Two things worth noticing here:

- **`query.copy().adjustSelect { … }`** — Exposed lets us start from the filtered `Query` and swap the `SELECT` clause without re-stating the WHERE. The same `toQuery()` that drives the list endpoint feeds four DISTINCT projections and a `MIN/MAX` range scan. There is no possibility of the options endpoint applying a slightly different filter than the search endpoint, because there is no second copy of the filter.
- **No predicate is duplicated** between `search()` and `options()`. If a `sessionType` filter is added tomorrow, the facets update automatically — the new `andWhere` flows through `query.copy()`.

A classic finder-method-per-screen repository would have this answer split across three or four method bodies, each with its own near-identical WHERE construction. Drift between them is the kind of subtle bug that produces "I selected this filter but the filter dropdown still offers options that don't match" — exactly the bug `options()` is preventing here.

---

## `aggregate()` — same trick for GROUP BY

`aggregate()` answers "for the same filtered set, give me bucketed counts per day/week/month, plus summed driving time". It's the data behind the overview dashboard's "Sessions per month" and "Driving time per month" charts.

```kotlin
fun aggregate(
  criteria: SessionSearchCriteria,
  groupBy: SessionAggregateGroupBy,
): List<SessionAggregateBucket> {
  val unit = groupBy.timeBucketUnit
  val truncExpr = dateTrunc(unit, SessionTable.startedAt)
  val countExpr = SessionTable.id.count()
  val sumExpr   = SessionTable.drivingTimeMs.sum()

  val q = criteria
    .toQuery()
    .andWhere { SessionTable.startedAt.isNotNull() }
    .adjustSelect { select(truncExpr, countExpr, sumExpr) }
    .groupBy(truncExpr)

  return q.map { row ->
    SessionAggregateBucket(
      key = formatTimeBucketKey(row[truncExpr], unit),
      count = row[countExpr],
      drivingTimeMs = row[sumExpr] ?: 0L,
    )
  }
}
```

It's the same shape as `options()`:

1. Start from `criteria.toQuery()`.
2. Narrow the projection (`SELECT DATE_TRUNC(...), COUNT(*), SUM(driving_time_ms)`).
3. Add the dimension (`groupBy(truncExpr)`).
4. Add the dimension-specific constraint (`startedAt IS NOT NULL` — a session with no start time has no timeline position and would bucket to a meaningless `null` key).

Both metrics — `count` and `drivingTimeMs` — come back from one round trip so the two dashboard charts share a single fetch. The dialect-specific bits (`DATE_TRUNC(...)`, bucket key formatting) are factored out into `AggregationSupport.kt` and reused by the lap aggregate too.

The repository doesn't grow per chart. The overview screen has four time-bucketed widgets driven by sessions; they all share this one method, varying only in the `groupBy`.

---

## Interfaces that capture cross-cutting capability

The repository implements three small interfaces:

```kotlin
class SessionRepository(...) :
  FindByIdRepository<SessionEntity, Long>,
  FindByCriteriaRepository<SessionEntity>,
  SearchRepository<SessionEntity, SessionSearchCriteria>
```

```kotlin
interface FindByIdRepository<T, ID> {
  fun findOneOrNull(id: ID): T?
}

fun <T, ID> FindByIdRepository<T, ID>.findOneOrThrow(id: ID): T =
  findOneOrNull(id) ?: throw NotFoundException()

interface SearchRepository<T, C> {
  fun searchForOne(criteria: C, sort: Sort = Sort.noSort()): T?
  fun search(criteria: C, pageRequest: PageRequest, sort: Sort = Sort.noSort()): Page<T>
}
```

The `findOneOrThrow` is an extension function on the interface, not a method on the implementation. Same with the equivalent on `FindByCriteriaRepository`. The repository doesn't have to copy that boilerplate — every repository that opts into `FindByIdRepository` automatically gets the throwing variant. It is the kind of small touch that prevents a `findById(...).orThrow()` helper from being re-implemented inline at five different call sites.

These interfaces also keep the *contract* consistent across aggregates: when you sit down to write the next repository for some new entity, the surface you should aim for is already named in the type system. `Lap` and `Session` repositories look almost identical at the interface level because they both implement the same trio.

---

## Where sortable fields live, and why

Sortable-field mapping is colocated with the entity, not the repository:

```kotlin
// adapter/out/persistence/session/SessionEntity.kt
companion object : LongEntityClass<SessionEntity>(SessionTable) {
  val sortableFields = SortableFields(
      mapOf(
        "startedAt"     to SessionTable.startedAt,
        "track"         to SessionTable.track,
        "car"           to SessionTable.car,
        "sessionType"   to SessionTable.sessionType,
        "simulator"     to SessionTable.simulator,
        "drivingTimeMs" to SessionTable.drivingTimeMs,
      )
    )
    .also {
      require(it.mapping.keys == Session.SORTABLE_FIELDS.toSet()) {
        "SessionEntity.sortableFields keys ${it.mapping.keys} must match " +
          "Session.SORTABLE_FIELDS ${Session.SORTABLE_FIELDS}"
      }
    }
}
```

Two reasons. First, it's the only place in the codebase that knows both the domain field names and the Exposed columns — putting it next to the entity keeps that translation visible. Second, the `require { }` block fails class initialisation if the domain's `Session.SORTABLE_FIELDS` and this mapping ever drift. A new sortable field can't be silently exposed without a column behind it, and a column can't be quietly removed without the domain noticing.

The repository never deals with `Map<String, Column<*>>` directly; it just passes `SessionEntity.sortableFields` into the shared `paginate` extension, which is where unknown field names get filtered out via `mapNotNull`.

---

## Where the boundary is drawn

A few small choices keep the repository genuinely focused:

- **Transactions live in the service**, not here. `SearchSessionService.searchSessions` wraps the port call in `transaction { … }`. The repository assumes it's already inside one, which is true for every caller and means tests can run a whole scenario inside a single explicit transaction without nested-transaction overhead.
- **Domain ↔ entity mapping lives in `SessionMapper`**, not in the repository. The repository deals in `SessionEntity`; the persistence adapter (`SessionPersistenceAdapter`) is the layer that calls `mapper::toDomain` on the way out. The repository can be exercised against the real database without a mapper involved, which makes integration tests cheap.
- **Aggregation key formatting lives in shared util** (`formatTimeBucketKey` in `AggregationSupport.kt`). The repository emits `Instant` from the SQL truncation and lets the formatter handle the dialect/TZ rules — same util that `LapRepository.aggregate` uses, so the wire keys are guaranteed to format the same way.
- **Reads return entities, not domain objects**. The adapter does the final mapping. Letting the repository handle the entity layer means lazy fields (`SessionEntity.startedAt`) and Exposed's DAO conveniences (`findByIdAndUpdate { … }`) are still available to `update`, without leaking them into the domain.

The total result is that the repository sits squarely between Exposed and the application's port contract, and doesn't do anything else.

---

## What this buys, concretely

The structure pays for itself the second or third time you reach for it:

- The session list, the option facets, and the aggregate dashboard — three endpoints, three response shapes — share *one* WHERE construction. Filter consistency is automatic.
- Adding a new search dimension is a one-line change to the criteria and one line in `toQuery()`. Every consumer benefits.
- A new search semantic is a new criteria type or a new `resolvedQuery()` branch, not a new repository method. Complexity is contained in named, testable functions.
- The repository's public method count is roughly stable as the app grows. Each new screen on the dashboard doesn't grow `SessionRepository` by a method.

The pattern isn't novel — Spring Data's `Specification`, JPA's `CriteriaQuery`, and various others encode the same idea. What's worth noting is how cheap it is to do by hand in Kotlin + Exposed: a data class, a single extension function on it, and a Query builder that knows how to copy itself. That's the whole machinery, and it's enough to keep the repository small for a long time.
