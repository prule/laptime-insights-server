## Why

The Public Profile (Driver Passport) page added by [add-public-profile] is driven by a hard-coded
bundled sample (`frontend/src/api/profile.ts`) and is statically force-enabled in the UI — a known
shortcut recorded in `docs/technical-debt.md`. To make it real, the **local install** must serve the
user's own profile snapshot, generated from their local data, and the user must be able to turn the
public profile **on or off** at runtime. Serving it locally also produces exactly the artifact a
subscriber would later upload (see [add-cloud-publish]), so the snapshot built here becomes the seam
between the local app and the future cloud-hosted page.

## What Changes

- Add a backend endpoint that serves the user's **profile snapshot JSON** (the `ProfileData`
  contract the frontend already expects), **generated from the local Session and Lap data** merged
  with signup identity. This is the same payload that would be uploaded when subscribed.
- Add a **runtime on/off toggle** for the public profile, persisted in the app's JSON config file
  (`ApplicationConfiguration`) so it survives restarts. Default on.
- Expose the toggle as a **HATEOAS action link in the index resource** (`GET /api/1`), and gate the
  `public-profile` feature the normal way: its capability `_link` + `enabledFeatures` flag are
  present only when the profile is enabled.
- Add signup **identity config** (name, slug, tagline, location, member-since) to the config file —
  the parts of the snapshot that aren't derivable from telemetry.
- **Frontend**: replace the bundled-sample accessor with a data-mode-keyed fetch that follows the
  HATEOAS link; toggle the page via the new action; **remove the statically-enabled hack** in
  `FeaturesProvider` and the fake `rel`, retiring the technical-debt item.
- Update docs (`docs/public-profile.md`, `docs/architecture.md`, user guide) and tests on both
  sides.

## Capabilities

### New Capabilities
- `public-profile-api`: End-to-end dynamic profile — backend generation + serving of the user's profile snapshot from local data, a runtime-persisted on/off toggle exposed via HATEOAS, the link/flag gating, and the frontend consuming it dynamically (replacing the bundled sample + static-enable shortcut from [add-public-profile]).

### Modified Capabilities
<!-- The `public-profile` page from add-public-profile is not yet archived to openspec/specs/,
     so there is no base spec to MODIFY. Its dynamic/gated behaviour is captured as ADDED
     requirements under `public-profile-api` and reconciled when add-public-profile is archived. -->

## Impact

- **Backend** (`app/`):
  - domain/application: a `ProfileSnapshot` model + a port to read the aggregates it needs; a
    snapshot-builder service composing identity (config) with totals / per-track laps / best-lap
    records (Session + Lap queries, reusing existing aggregate read paths where possible).
  - adapter/in/web: a `public-profile` controller serving `GET` snapshot JSON and a toggle action
    (`PUT`/`POST`); register routes in `App.kt`.
  - index: add `publicProfile` (data) + a profile-toggle (action) link to `IndexLinkFactory`, and
    make `public-profile` a runtime-resolved member of `enabledFeatures` rather than env-only.
  - config: extend `ApplicationConfiguration` with a `publicProfile` block (enabled flag + identity);
    persist toggle writes back to the JSON config file (new write path on the config repository).
  - `Feature` enum: add `PUBLIC_PROFILE`.
- **Frontend** (`frontend/src/`):
  - `api/profile.ts`: snapshot fetch (data-mode keyed, follows `_links.publicProfile`); keep the
    sample only as a MOCK fixture / test data.
  - toggle control wired to the action link; `PublicProfileScreen` consumes fetched data with
    loading/error states.
  - `providers/FeaturesProvider.tsx`: remove `STATICALLY_ENABLED`; `config/features.tsx`: real `rel`.
- **Docs**: `docs/public-profile.md` (now dynamic), `docs/architecture.md` (config-persisted runtime
  toggle), user guide; mark the static-enable item **DONE** in `docs/technical-debt.md`.
- **Relationship to [add-cloud-publish]**: that change's outbound upload SHOULD send the snapshot
  built here; this change owns generation + local serving + the toggle, not the cloud POST.
- No DB schema change (reads only). No new inbound auth.
