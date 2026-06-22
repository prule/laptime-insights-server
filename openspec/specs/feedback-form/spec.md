# feedback-form Specification

## Purpose

Provide an always-available feedback channel in the dashboard that lets users submit a
typed message (bug, suggestion, or general feedback) with optional contact email,
auto-attaching app context and submitting directly to a configured Google Form without a
backend dependency.

## Requirements

### Requirement: Feedback launcher in the dashboard shell

The dashboard SHALL present a persistent **Feedback** launcher control in the app shell
that is reachable from every screen. The launcher MUST NOT depend on any data feature
toggle (`enabledFeatures`) and MUST open the feedback form without navigating away from
the current screen.

#### Scenario: Launcher visible on every screen

- **WHEN** a user is on any dashboard screen with feedback configured
- **THEN** a Feedback launcher control is visible in the app shell
- **AND** activating it opens the feedback form modal over the current screen

#### Scenario: Launcher hidden when unconfigured

- **WHEN** no Google Form endpoint is configured for feedback
- **THEN** the Feedback launcher is not rendered
- **AND** no feedback modal can be opened

### Requirement: Feedback form fields

The feedback form SHALL capture a feedback **type** (one of: bug, suggestion, general
feedback), a free-text **message**, and an optional **email** for follow-up. The type
MUST default to a defined value and the message MUST be the only field the user is
required to fill.

#### Scenario: Form presents all fields

- **WHEN** the feedback form opens
- **THEN** a type selector, a message text area, and an optional email input are shown
- **AND** the type selector has a pre-selected default value

#### Scenario: Email is optional

- **WHEN** a user submits with a valid message and an empty email
- **THEN** submission proceeds
- **AND** no validation error is shown for the email field

### Requirement: Auto-attached context

On submission the feedback form SHALL include contextual metadata that the user does not
type: the **app version** and the **current screen** (route) at the time the form was
opened. This context MUST be sent alongside the user-entered fields.

#### Scenario: Context included in submission

- **WHEN** a user submits feedback from a given screen
- **THEN** the submitted payload includes the app version and that screen's identifier
- **AND** the user was not required to enter that context manually

### Requirement: Client-side validation

The feedback form SHALL validate input on the client before submitting. A blank or
whitespace-only message MUST NOT be submitted and MUST produce an inline validation
message. When an email is provided it MUST be syntactically valid or submission is
blocked with an inline message.

#### Scenario: Empty message rejected

- **WHEN** a user submits with an empty or whitespace-only message
- **THEN** submission is blocked
- **AND** an inline message prompts the user to enter a message

#### Scenario: Invalid email rejected

- **WHEN** a user provides a non-empty, syntactically invalid email (e.g. `foo@`)
- **THEN** submission is blocked
- **AND** an inline validation message is shown

#### Scenario: Valid input accepted

- **WHEN** a user submits a non-empty message with either no email or a valid email
- **THEN** validation passes and the submission is attempted

### Requirement: Submit to configured Google Form

On a valid submission the form SHALL send the feedback to a configured Google Form via a
`no-cors` POST to the form's `formResponse` endpoint, mapping each field (type, message,
email, app version, screen) to its configured form `entry.<id>`. The endpoint URL and
entry-id mappings SHALL come from frontend build-time configuration. The user MUST NOT be
navigated away from the dashboard to submit.

#### Scenario: Feedback reaches the Google Form

- **WHEN** a user submits valid feedback in LIVE data mode
- **THEN** a `no-cors` POST is sent to the configured `formResponse` URL with each field mapped to its configured entry id
- **AND** the user remains on the current screen

#### Scenario: No backend dependency

- **WHEN** the feedback feature operates
- **THEN** it submits directly to the Google Form with no call to the `app/` REST API
- **AND** introduces no new server-side runtime requirement

### Requirement: Mock-mode submission is simulated

When the dashboard is in MOCK data mode the feedback form SHALL NOT issue a real network
request. Submission MUST be simulated locally so the form can be exercised offline,
ending in the success state.

#### Scenario: No network call in mock mode

- **WHEN** a user submits valid feedback while in MOCK data mode
- **THEN** no request is sent to the Google Form endpoint
- **AND** the form transitions to its success state

### Requirement: Submission feedback states

The feedback form SHALL give inline feedback for in-progress, success, and failure
states. Because a `no-cors` request returns an opaque response, a completed request
without a thrown network error SHALL be treated as success. On success the form MUST
prevent duplicate submission; on failure it MUST allow retry.

#### Scenario: In-progress state

- **WHEN** a submission is underway
- **THEN** the submit control indicates progress and is disabled to prevent duplicate sends

#### Scenario: Success confirmation

- **WHEN** the submission completes without a network error
- **THEN** an inline success message is shown
- **AND** the form is reset or disabled to prevent duplicate submission

#### Scenario: Failure allows retry

- **WHEN** the submission throws a network error
- **THEN** an inline failure message is shown
- **AND** the user can retry submission without losing their entered message
