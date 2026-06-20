## ADDED Requirements

### Requirement: Account sign-in and unique username

The service SHALL let a user sign in and claim exactly one unique username per account.
The username MUST be unique across all accounts and MUST be immutable once claimed.

#### Scenario: Sign in and claim username

- **WHEN** a user signs in for the first time and chooses an available username
- **THEN** an account is created bound to that username
- **AND** the username is reserved against reuse by any other account

#### Scenario: Duplicate username rejected

- **WHEN** a user attempts to claim a username already held by another account
- **THEN** the claim is rejected

### Requirement: Revocable publish token

The service SHALL issue an API token to an account for use as the local app's publish
credential. The raw token SHALL be shown exactly once and stored only as a hash. The
account owner SHALL be able to regenerate the token, which MUST invalidate the previous
token.

#### Scenario: Token issued once

- **WHEN** an account's token is generated
- **THEN** the raw token is displayed once to the owner
- **AND** only a hash of the token is stored

#### Scenario: Regenerate invalidates the old token

- **WHEN** the owner regenerates the token
- **THEN** a new token is issued
- **AND** publishes presenting the previous token are rejected

### Requirement: Subscription gates visibility

A paid subscription ($12/year) SHALL set the account's expiry. Subscription state SHALL
gate both acceptance of publishes and rendering of the public page. Payment completion
SHALL set the expiry via the payment provider's webhook.

#### Scenario: Successful payment sets expiry

- **WHEN** a checkout completes successfully
- **THEN** the account's expiry is set to the end of the paid period via the payment webhook

#### Scenario: Expired subscription pauses the page

- **WHEN** the current time is past an account's expiry
- **THEN** the account's public page renders a "subscription expired / paused" state
- **AND** the account's stored summary data is retained for restoration on renewal

### Requirement: Token-authenticated publish receiver

The service SHALL accept `POST /publish` authenticated by a `Bearer` token. It SHALL
resolve the token to an account, require an active subscription, and replace that
account's stored summary with the posted snapshot (full-snapshot replace).

#### Scenario: Valid publish stored

- **WHEN** a POST to `/publish` arrives with a Bearer token matching an account with an active subscription
- **THEN** the account's activity totals and best-lap-per-track/car set are replaced with the posted payload
- **AND** a 2xx response is returned

#### Scenario: Unknown or revoked token rejected

- **WHEN** a publish presents a token that matches no current account token
- **THEN** the request is rejected as unauthorized
- **AND** no data is stored

#### Scenario: Expired subscription rejects publish

- **WHEN** a publish presents a valid token for an account whose subscription has expired
- **THEN** the publish is rejected
- **AND** the prior stored summary is left unchanged

### Requirement: Public dashboard page

The service SHALL serve a public page at `/dashboard/<username>` rendering the account's
activity totals and best lap per track/car. An unknown username MUST return not-found; an
expired subscription MUST render the paused state instead of the data.

#### Scenario: Active account renders stats

- **WHEN** a visitor requests `/dashboard/<username>` for an account with an active subscription and published data
- **THEN** the page renders activity totals (sessions, laps, distance, driving time) and the best lap for each track/car combination

#### Scenario: Unknown username

- **WHEN** a visitor requests `/dashboard/<username>` for a username that does not exist
- **THEN** a not-found response is returned

#### Scenario: Publicly accessible

- **WHEN** any visitor (signed in or not) requests an active account's dashboard URL
- **THEN** the page is served without requiring the visitor to authenticate
