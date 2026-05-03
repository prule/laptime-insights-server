# LapTime Insights — User Guide

LapTime Insights is a local dashboard that connects to your Assetto Corsa Competizione (ACC) server and gives you a clear picture of your practice history and lap performance. This guide covers every screen in the dashboard and how to get the most out of it.

---

## Getting around

The sidebar on the left contains four screens: **Overview**, **Sessions**, **Laps**, and **Compare**. Click any label to navigate there. The active screen is highlighted.

At the top of every screen is the **time range selector** — a row of pill buttons labelled `1M`, `3M`, `6M`, `1Y`, and `All`. This filter applies globally: every stat, chart, and list on every screen is restricted to data recorded within the selected window. Your choice is saved across browser reloads.

| Button | Window |
|--------|--------|
| 1M | Last 30 days |
| 3M | Last 3 months |
| 6M | Last 6 months |
| 1Y | Last year |
| All | All recorded data |

---

## Overview screen

The Overview screen gives you a single-glance summary of your driving activity.

### Header stats

Four stat cards sit at the top of the page:

- **Total Sessions** — how many sessions fall inside the active time range.
- **Total Laps** — total lap count for the range.
- **Best Lap** — your fastest valid lap across all sessions in the range.
- **Avg Lap** — mean lap time across all valid laps in the range.

The header also displays your **Personal Best** in large text in the top-right corner.

### Activity charts

Two bar charts show your pace of activity over the active range:

- **Laps per week / month** — how many laps you recorded in each bucket.
- **Sessions per week / month** — how many sessions you ran.

Short ranges (1M, 3M) use weekly buckets; longer ranges (6M, 1Y, All) switch to monthly buckets so the chart stays readable.

### Tracks practiced

A bubble chart shows every track you have ever visited. The **area of each bubble is proportional to the number of laps** recorded at that track within the active time range. Tracks with zero laps in the range are shown as dashed outlines — useful for spotting tracks you haven't been back to recently.

### Recent sessions

A list of your four most recent sessions is shown at the bottom. Click **View all** to jump to the Sessions screen, or click any session row to open its detail page.

---

## Sessions screen

The Sessions screen lists all your recorded sessions with filtering by track, car, and simulator.

### Filters

Three drop-down selects let you narrow the list:

- **Track** — restrict to sessions at a specific circuit.
- **Car** — restrict to sessions with a specific car.
- **Simulator** — restrict to sessions from a specific game (e.g. ACC).

The global time range selector also applies — sessions outside the window are excluded regardless of the other filters.

Filters are written into the page URL, so you can bookmark or share a filtered view. The **Clear** button removes all active filters at once.

The header beneath the filters shows the match count and how many rows are being displayed (up to 50 per page).

### Session rows

Each row shows the track, car, session type (Practice / Qualifying / Race), simulator, start time, and duration. Click a row to open the Session Detail screen for that session.

---

## Session Detail screen

Reached by clicking any session row. Shows full information about one session.

### Session header

Displays the track name, car, session type badge, simulator, start date/time, and total duration.

### Summary stats

Four stat cards show:

- **Laps** — total lap count for this session.
- **Valid** — number of laps that were not flagged invalid.
- **Best Lap** — fastest valid lap in the session.
- **Avg Lap** — mean time across valid laps.

A sparkline trend chart in the section header shows how your lap times evolved across the session.

### Lap table

Every lap recorded in the session is listed in order. Each row shows:

- **Lap number**
- **Recorded time** (clock time when the lap completed)
- **Lap time** — green if it is your all-time personal best at this track
- **Δ to best** — how far this lap is from the session's fastest valid lap (positive = slower)
- **Status** — `PB` (personal best) or `INVALID`

#### Comparing from the lap table

Each valid lap row has two compare buttons:

- **vs best** — compare this lap against the session's fastest valid lap. Disabled when this lap is the fastest.
- **vs PB** — compare this lap against your all-time fastest valid lap at this track. Disabled when this lap is itself the track PB.

Clicking either button takes you straight to the Compare screen with both laps pre-selected — no picking required.

---

## Laps screen

The Laps screen shows a searchable, paginated list of all your laps across every session.

### Filters

- **Track**, **Car**, **Simulator** drop-downs narrow the list.
- **Valid only** toggle (on by default) hides invalid laps.
- **Personal bests** toggle shows only your personal best per session.
- The global time range selector applies here too.
- **Reset** removes all active filters and toggles.

The filter state is written to the URL — a link like `/laps?track=Monza&pb=true&page=2` restores the same view.

### Lap table

Laps are sorted fastest first. Each row shows rank, lap time (green if PB), track, car, simulator, recorded time, and status. Click any row to open its parent session.

Pagination is shown at the bottom (Prev / Next). The header reports the total match count and current page.

### Selecting laps to compare

Click **Select to compare** to enter multi-select mode. A checkbox column appears. Tick any two laps, then click **Compare selected** — you land on the Compare screen with both laps pre-loaded. The button is disabled until exactly two laps are ticked. Click **Cancel** to exit select mode without navigating.

---

## Compare screen

The Compare screen overlays telemetry from two laps so you can see exactly where time is gained or lost.

### Picking laps

Two **lap pickers** sit at the top of the page. Each picker is a button that opens a searchable modal:

- Filter the modal list by track, car, PB-only, and valid-only.
- Paginate through results.
- Click a row to select that lap.

When you jump to Compare from a Session Detail row (via **vs best** or **vs PB**) both pickers are already filled in — you don't need to pick manually. The optional `track` hint from the source session pre-filters the pickers if you decide to swap one of the laps.

The comparison is fully shareable: the URL contains `lap1`, `lap2`, and optionally `track`, so copying the address bar gives a link that restores the identical comparison.

### Telemetry charts

Once two laps are selected the following charts render:

- **Speed (KPH)** — both laps' speed traces overlaid, plotted against `splinePosition` (0 = start/finish, 1 = end of lap). Lap 1 is cyan, Lap 2 is red.
- **Speed delta** — Lap 1 minus Lap 2 at every 1% of track length. Above zero means Lap 1 was faster at that point; below zero means Lap 2 was faster. Use this to find the specific corners or straights where you are losing time.
- **Gear mismatch** — a strip that highlights (in red) every sector of the track where the two laps used a different gear. A mismatch is not always bad — it may indicate an opportunity to shift strategy.
- **Throttle** — throttle input (0–1) for both laps.
- **Brake** — brake input (0–1) for both laps.

Each lap's time and lap number are shown at the top of the results area. A `PB` badge appears if that lap is an all-time personal best at the track.

---

## Data modes

The bottom-left corner of the sidebar contains a **MOCK / LIVE** toggle:

- **MOCK** (default) — the dashboard uses an in-memory dataset that mirrors the backend's database seeder. No server required. Useful for exploring the UI or developing the frontend.
- **LIVE** — the dashboard makes real API calls to the backend. Requires the Ktor server to be running. Toggle this when the server is active and you want to see real recorded data.

Switching modes re-fetches all data; cached data from the previous mode is never mixed in.
