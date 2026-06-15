## ADDED Requirements

### Requirement: Isolated landing site
The landing page SHALL live in a dedicated top-level `landing/` directory and MUST NOT depend
on `app/` or `frontend/` source, build, or runtime. It SHALL build to static assets (HTML, CSS,
images) with no server-side runtime.

#### Scenario: Build produces static output
- **WHEN** the landing build command runs in `landing/`
- **THEN** a static output directory (e.g. `landing/dist`) is produced containing HTML, CSS, and assets
- **AND** no `app/` or `frontend/` files are read or modified

#### Scenario: App build unaffected
- **WHEN** the existing `frontend/` or `app/` build runs
- **THEN** it succeeds without referencing anything in `landing/`

### Requirement: Promotional content sections
The landing page SHALL present, on a single responsive page, a hero with a headline and primary
call-to-action, a feature section covering the dashboard's core value (Overview, Sessions, Laps,
Compare, streaks, and global time-range analytics), and at least one product screenshot.

#### Scenario: Hero with call-to-action
- **WHEN** a visitor loads the landing page
- **THEN** a headline, supporting subtext, and a primary CTA button linking to the app are visible above the fold

#### Scenario: Feature highlights present
- **WHEN** a visitor scrolls the page
- **THEN** feature sections describe core capabilities (Overview, Sessions, Laps, Compare, streaks, time-range filtering)
- **AND** at least one screenshot of the dashboard is shown

### Requirement: Responsive layout
The landing page SHALL render correctly on mobile, tablet, and desktop widths using Tailwind CSS,
with no horizontal overflow.

#### Scenario: Mobile viewport
- **WHEN** the page is viewed at 375px width
- **THEN** content stacks vertically with readable text and no horizontal scrolling

#### Scenario: Desktop viewport
- **WHEN** the page is viewed at 1280px width
- **THEN** multi-column feature layouts render as designed

### Requirement: SEO and social metadata
The landing page SHALL include a descriptive `<title>`, meta description, Open Graph and Twitter
card tags, a favicon, a `robots.txt`, and a `sitemap.xml`. Social tags MUST reference an absolute
share image URL.

#### Scenario: Crawlable and shareable
- **WHEN** the page source is inspected
- **THEN** title, meta description, canonical URL, Open Graph tags, and Twitter card tags are present

#### Scenario: Crawler files served
- **WHEN** `/robots.txt` and `/sitemap.xml` are requested
- **THEN** both return valid content listing the site

### Requirement: Cloudflare Pages deployment
The landing site SHALL be deployable to Cloudflare Pages with a documented build command and
output directory, independent of the existing backend/frontend CI pipelines.

#### Scenario: Deploy configuration documented
- **WHEN** a developer reads `landing/README.md`
- **THEN** it states the Cloudflare Pages build command, output directory, and local preview steps

#### Scenario: Production build succeeds for Pages
- **WHEN** the documented build command runs
- **THEN** it exits successfully and emits the configured output directory ready for Pages upload
