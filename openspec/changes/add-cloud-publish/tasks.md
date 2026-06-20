# Tasks — add-cloud-publish (local app)

Ordered by dependency. Each task is small enough for one session and includes its doc/test updates.

## Domain & ports

- [ ] 1. Add domain model `PublishSummary` (totals + list of best-lap-per-track/car) under
  `application/domain/model`. Unit-test its construction/value semantics.
- [ ] 2. Add output port to read aggregates (e.g. `ReadActivityTotalsPort`,
  `ReadBestLapsPort`) under `application/port/out`.
- [ ] 3. Add output port for publishing (`PublishSummaryPort`) under
  `application/port/out`.

## Aggregation (persistence read side)

- [ ] 4. Implement Exposed read queries for activity totals (session count, lap count,
  total distance, total driving time) in `adapter/out/persistence`. Test against H2.
- [ ] 5. Implement Exposed read query for best lap per (track, car) over valid laps.
  Test grouping + valid-only filtering against H2 seed data.

## Use case

- [ ] 6. Add `PublishSummaryUseCase` (input port + interactor) in `application` that
  builds the snapshot via the read ports and sends it via `PublishSummaryPort`. Test the
  orchestration with fakes (enabled vs disabled, success vs failure result).

## Configuration & infrastructure

- [ ] 7. Add publish configuration (`PUBLISH_CLOUD_URL`, `PUBLISH_TOKEN`,
  `PUBLISH_INTERVAL`) with parsing/validation; enabled only when URL + token both set.
  Test the enabled/partial/disabled resolution.
- [ ] 8. Implement the outbound HTTP adapter for `PublishSummaryPort` using the Ktor
  client: POST `{cloudBaseUrl}/publish`, `Bearer` token, JSON body; map 2xx→success,
  else→failure. Add the Ktor client dependency to the version catalog. Test against a
  stub server (request shape, auth header, result mapping).
- [ ] 9. Add the periodic scheduler (coroutine) that invokes the use case on the
  configured interval; starts only when enabled and interval != off. Test start/no-start
  conditions.
- [ ] 10. Wire everything in `AppModule.kt` (manual DI).

## Web adapter (inbound trigger + status)

- [ ] 11. Add a route to trigger a manual publish and to read publish status (last
  published time, last result, enabled flag). REST-assured test for trigger + status.
- [ ] 12. Add the publish capability `_link` and `enabledFeatures.cloudPublish` to the
  relevant resource presenter, present only when enabled. Test presence/absence.

## Frontend

- [ ] 13. Add API types + TanStack Query hooks for publish status and the manual publish
  mutation, gated on the capability link / `enabledFeatures`.
- [ ] 14. Add a "Publish now" control + publish-status display in the dashboard, hidden
  when the capability is absent. Add to mock layer so MOCK mode mirrors backend. Vitest
  coverage for visible/hidden + success/error states.

## Documentation

- [ ] 15. Update `docs/architecture.md` to describe the optional one-way cloud-publish
  boundary and that it preserves the self-hosted, local-source-of-truth model.
- [ ] 16. Update `docs/user-guide.md` with how to pair (paste token), publish manually,
  and set a periodic interval.
- [ ] 17. Note any follow-ups/cleanups in `docs/technical-debt.md`.
- [ ] 18. Confirm `docs/public-dashboard/` (separate project seed) references the same
  `/publish` contract as `specs/cloud-publish/spec.md`.
