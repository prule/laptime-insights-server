# LapTime Insights — User Guide

LapTime Insights is a **self-hosted** dashboard: you download it and run it on your own network, on the same network as your Assetto Corsa Competizione (ACC) server. It connects to that server, records your sessions and laps locally, and gives you a clear picture of your practice history and lap performance — your telemetry never leaves your network. You open the dashboard in a browser from your PC or any device on the same network. This guide covers every screen in the dashboard and how to get the most out of it.

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

Also in the top bar is a **Feedback** button. Use it to report a bug or send a suggestion
without leaving the dashboard: pick a type, write your message, and optionally leave an
email if you'd like a reply. Your app version and current screen are attached automatically
to help with diagnosis. (Available only when the operator has configured a feedback form.)

---

## Overview screen

The Overview screen gives you a single-glance summary of your driving activity.

### Header stats

Four stat cards sit at the top of the page:

- **Total Sessions** — how many sessions fall inside the active time range.
- **Total Laps** — total lap count for the range.
- **Driving Time** — total on-track time for your car across all sessions in the range (sum of player-car lap times; pit/idle periods are excluded).

The header also displays your **Streak** in the top-right corner — the number of consecutive calendar days, ending on your most recent session, on which you drove. The label beneath the number shows when the streak last ticked: `today` or `yesterday` means it's still alive (green), anything older means the streak has ended (muted) and the badge shows the date it broke.

### Activity charts

Three bar charts show your pace of activity over the active range — order mirrors the stat cards above:

- **Sessions per week / month** — how many sessions you ran in each bucket.
- **Laps per week / month** — how many laps you recorded in each bucket.
- **Driving time per week / month** — total on-track time in each bucket.

Short ranges (1M, 3M) use weekly buckets; longer ranges (6M, 1Y, All) switch to monthly buckets so the chart stays readable.

### Tracks practiced

A bubble chart shows every track you have ever visited. The **area of each bubble is proportional to the number of laps** recorded at that track within the active time range. Tracks with zero laps in the range are shown as dashed outlines — useful for spotting tracks you haven't been back to recently.

### All-time best per track

A table beneath the bubble chart lists the **fastest valid lap you've ever driven at each track**, one row per track. Unlike the rest of the Overview screen, this table is not bound to the active time range — "all-time" means all-time. Each row shows the track, the best lap time, the car you drove that lap in, and the date it was recorded. Click any row to jump to the detail page of the session in which that lap was driven.

### Recent sessions

A list of your four most recent sessions is shown at the bottom. Click **View all** to jump to the Sessions screen, or click any session row to open its detail page.

---

## Sessions screen

The Sessions screen lists all your recorded sessions with filtering by track, car, and simulator.

Each session corresponds to a single session in ACC. When you finish a session and start another — for example two races back-to-back — they are recorded as separate sessions automatically, even if the dashboard stays connected the whole time. A session is closed when ACC moves to a new session or reaches the end-of-session screen, so a new race never gets appended to the previous one.

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

You only ever compare laps **on the same track**, so the track is the shared axis at the top of the page — pick it once and both laps come from it. On a fresh visit the screen seeds itself from your **latest session**: that session's track becomes the axis and your fastest lap there becomes the **anchor** (your reference lap). If you set no laps of your own in that session, the anchor falls back to the session's best lap, so the screen always lands on something.

The two slots are an **anchor** and a **challenger**:

- **Anchor** — your reference lap, shown top-left and marked *auto* when it's the seeded default. Click it to change it to any other lap on the track.
- **Challenger** — picked from a **ranked leaderboard** of laps on the track, fastest first. Each row shows its rank (`#`), lap time, a dot marking whether the lap is yours, and which session it came from. Click a row to compare it against the anchor.

The leaderboard has three toggles:

- **This session / All sessions** — stay within the seeding session, or open up to every session at the track.
- **Me / Field** — only your laps, or everyone's.
- **Same car** (on by default) — restrict to the same car as the anchor, so the gap is driver skill rather than machinery. Turn it off to see all cars.

So "my fastest vs this session's fastest" and "my fastest vs my 2nd fastest at this track" are just different toggle states. Changing the track clears the anchor and challenger and re-seeds the default, since they can't carry over to a different circuit.

When you jump to Compare from a Session Detail row (via **vs best** or **vs PB**) the anchor and challenger are already filled in — no picking needed.

The comparison is fully shareable: the URL contains `track`, `anchor`, and `challenger`, so copying the address bar gives a link that restores the identical comparison. (Older links using `lap1`/`lap2` still work.)

### Telemetry charts

Once two laps are selected the following charts and panels render:

- **Speed (KPH)** — both laps' speed traces overlaid, plotted against track position (0 = start/finish, 1 = end of lap). The anchor is cyan, the challenger is red.
- **Track map** — a 2-D outline of the track derived from the cars' world coordinates. As you hover over any chart, a coloured dot per lap moves along the track outline to show exactly where on the circuit that point corresponds to. You can also hover directly over the track map to drive the charts.
- **Speed delta** — anchor minus challenger at every 1% of track length. Above zero means the anchor was faster at that point; below zero means the challenger was faster. Use this to find the specific corners or straights where you are losing time.
- **Gear mismatch** — a strip that highlights (in red) every sector of the track where the two laps used a different gear. A mismatch is not always bad — it may indicate an opportunity to shift strategy.

All panels are linked: hovering over any chart draws a vertical crosshair across every other chart at the same track position simultaneously, and moves the dots on the track map. This makes it easy to correlate a speed loss in the delta chart with the gear used and the exact corner on the map.

Each lap's time and lap number are shown at the top of the results area. A `PB` badge appears if that lap is an all-time personal best at the track.

---

## Data modes

The bottom-left corner of the sidebar contains a **MOCK / LIVE** toggle:

- **MOCK** (default) — the dashboard uses an in-memory dataset that mirrors the backend's database seeder. No server required. Useful for exploring the UI or developing the frontend.
- **LIVE** — the dashboard makes real API calls to the backend. Requires the Ktor server to be running. Toggle this when the server is active and you want to see real recorded data.

Switching modes re-fetches all data; cached data from the previous mode is never mixed in.
