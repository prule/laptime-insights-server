## ADDED Requirements

### Requirement: Opt-in publish configuration

The local app SHALL treat cloud publishing as opt-in and disabled by default. Publishing
SHALL be enabled only when both a cloud base URL and an API token are configured. With no
such configuration the app MUST behave exactly as a non-publishing install and MUST NOT
make any outbound network request.

#### Scenario: Disabled by default

- **WHEN** the app starts with no publish URL or token configured
- **THEN** publishing is disabled
- **AND** no outbound publish request is ever made
- **AND** local recording and dashboard behaviour are unchanged

#### Scenario: Enabled when paired

- **WHEN** both a cloud base URL and an API token are configured
- **THEN** publishing is enabled
- **AND** the publish capability is exposed to the frontend

#### Scenario: Partial configuration stays disabled

- **WHEN** only one of the cloud URL or the token is configured
- **THEN** publishing remains disabled

### Requirement: Aggregated summary from local data only

The app SHALL compute the publish summary exclusively from the local Session and Lap
data. It MUST NOT include raw telemetry samples or per-tick realtime car updates. The
summary SHALL contain activity totals and a best lap time for each track/car combination.

#### Scenario: Totals computed

- **WHEN** a summary is built
- **THEN** it includes session count, lap count, total distance, and total driving time derived from Session and Lap rows

#### Scenario: Best lap per track/car uses valid laps only

- **WHEN** the best-lap-per-combination set is built
- **THEN** for each distinct (track, car) the minimum lap time over laps marked valid is included
- **AND** invalid laps are excluded

#### Scenario: No raw telemetry leaves the network

- **WHEN** a summary is built and published
- **THEN** no raw telemetry samples or realtime car update rows are included in the payload

### Requirement: Token-authenticated full-snapshot publish

On publish the app SHALL send the summary to `{cloudBaseUrl}/publish` as an HTTP POST
with the configured token as a `Bearer` Authorization credential and the summary as the
JSON body. Each publish SHALL carry the user's complete current summary as a full
snapshot intended to replace any prior data for the account.

#### Scenario: Publish request shape

- **WHEN** a publish is triggered while enabled
- **THEN** a POST is sent to the configured cloud base URL `/publish` path
- **AND** the request carries the configured token as a `Bearer` Authorization header
- **AND** the JSON body contains `publishedAt`, `totals`, and `bests`

#### Scenario: Snapshot is complete and idempotent

- **WHEN** two publishes occur with no intervening data change
- **THEN** both send equivalent complete-summary payloads
- **AND** neither relies on the cloud retaining prior partial state

#### Scenario: Result recorded

- **WHEN** the cloud responds with a 2xx status
- **THEN** the publish is recorded as successful with its timestamp
- **WHEN** the cloud responds with a non-2xx status or the request fails
- **THEN** the publish is recorded as failed and the failure is reportable to the user

### Requirement: Manual and periodic triggers share one publish action

The app SHALL provide a manual trigger to publish on demand and a configurable periodic
trigger. Both SHALL invoke the same publish action producing the same request. The
periodic trigger SHALL run only when an interval other than "off" is configured and
publishing is enabled.

#### Scenario: Manual publish

- **WHEN** a user invokes the manual publish trigger while publishing is enabled
- **THEN** a publish request is sent and its result is returned to the caller

#### Scenario: Periodic publish

- **WHEN** a publish interval of `15m`, `1h`, or `daily` is configured and publishing is enabled
- **THEN** the app publishes automatically on that interval using the same action as the manual trigger

#### Scenario: Periodic off by default

- **WHEN** the publish interval is `off` or unset
- **THEN** no automatic publishing occurs
- **AND** manual publishing remains available when enabled

### Requirement: Publish capability exposed via HATEOAS

The app SHALL expose the publish capability to the frontend following the project's
HATEOAS convention: a capability `_link` (and corresponding `enabledFeatures` toggle)
present only when publishing is enabled, so the frontend follows the link rather than
hardcoding the route and hides the control when publishing is unconfigured.

#### Scenario: Capability present when enabled

- **WHEN** publishing is enabled and the frontend requests the relevant resource
- **THEN** a publish capability link and its `enabledFeatures` flag are present
- **AND** publish status (last published time and last result) is available to the frontend

#### Scenario: Capability absent when disabled

- **WHEN** publishing is disabled
- **THEN** the publish capability link is absent
- **AND** the frontend does not present a publish control
