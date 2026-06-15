## ADDED Requirements

### Requirement: Register-interest capture section

The landing page SHALL present a "Register interest" section containing a single email
input and a submit control, allowing a visitor to join a waiting list without leaving
the page. The section MUST be reachable on the single responsive landing page and MUST
NOT require the visitor to download or run the app first.

#### Scenario: Section is present

- **WHEN** a visitor loads the landing page
- **THEN** a "Register interest" section with an email input and a submit button is visible

#### Scenario: Secondary to download CTA

- **WHEN** a visitor views the page
- **THEN** the register-interest section is presented as an additional call-to-action
- **AND** the primary download/releases call-to-action remains present and unchanged

### Requirement: Client-side email validation

The register-interest form SHALL validate the email address on the client before
submitting. An empty or syntactically invalid email MUST NOT be submitted and MUST
produce an inline validation message.

#### Scenario: Empty email rejected

- **WHEN** a visitor submits the form with an empty email field
- **THEN** submission is blocked
- **AND** an inline message prompts the visitor to enter a valid email

#### Scenario: Invalid email rejected

- **WHEN** a visitor submits a value that is not a syntactically valid email (e.g. `foo@`)
- **THEN** submission is blocked
- **AND** an inline validation message is shown

#### Scenario: Valid email accepted

- **WHEN** a visitor submits a syntactically valid email
- **THEN** validation passes and the submission is attempted

### Requirement: Submit to Google Sheet waiting list

On a valid submission the page SHALL send the email to a Google Form via a `no-cors`
POST to the form's `formResponse` endpoint, mapping the email to the configured form
entry field. The Google Form's linked Google Sheet SHALL serve as the waiting list. The
visitor MUST NOT be navigated away from the landing page to submit.

#### Scenario: Email reaches the waiting list

- **WHEN** a visitor submits a valid email
- **THEN** a `no-cors` POST is sent to the configured Google Form `formResponse` URL with the email mapped to the configured entry field id
- **AND** the visitor remains on the landing page

#### Scenario: Static site preserved

- **WHEN** the landing site is built and deployed
- **THEN** the feature works with static assets only, with no backend or server-side runtime
- **AND** no dependency on `app/` or `frontend/` is introduced

### Requirement: Submission feedback states

The register-interest form SHALL give the visitor inline feedback for in-progress,
success, and failure states. Because a `no-cors` request returns an opaque response, a
completed request without a thrown network error SHALL be treated as success.

#### Scenario: Success confirmation

- **WHEN** the submission request completes without a network error
- **THEN** an inline success message (e.g. "You're on the list") is shown
- **AND** the form is reset or disabled to prevent duplicate submissions

#### Scenario: Network failure

- **WHEN** the submission request throws a network error
- **THEN** an inline error message is shown inviting the visitor to retry
- **AND** the email value is preserved so the visitor can resubmit

### Requirement: Configurable form endpoint

The Google Form URL and email entry field id SHALL be defined as clearly-marked
configuration values in one place, separate from presentation markup, so they can be set
once the form and sheet exist without restructuring the feature. Until configured, the
placeholders MUST be obvious (not a real-looking URL).

#### Scenario: Placeholder until configured

- **WHEN** the form endpoint has not yet been configured
- **THEN** the form URL and entry id exist as clearly-marked placeholder constants in a single location

#### Scenario: Configuration without code restructure

- **WHEN** a maintainer sets the real form URL and entry id
- **THEN** only those configuration values change, with no change to the validation or feedback logic
