Perfect. A **Dark/Stealth** aesthetic combined with a **Dual-Axis Toggle (Time vs. Distance)** is exactly how
professional data acquisition systems (like MoTeC or McLaren Applied) function.

Here is the refined UI specification for the LapTimeInsights Dashboard.

---

## 1. Visual Identity: "The Stealth Cockpit"

* **Surface Colors:** * `Background`: `#0A0A0B` (Near black)
    * `Card/Panel`: `#141416` (Slightly lighter, raised surface)
    * `Border`: `#232326` (Subtle separation)
* **Accent Colors:**
    * `Primary`: `#3B82F6` (Electric Blue - for active states/toggles)
    * `Success`: `#10B981` (Emerald - for valid laps/time gain)
    * `Warning`: `#EF4444` (Rose - for invalidated laps/time loss)
* **Interactivity:** Use a "Glassmorphism" effect for overlays (e.g., tooltips with `backdrop-filter: blur(8px)`).

---

## 2. Telemetry Engine: The Sync-Chart System

To handle the **Time/Distance** toggle, we’ll use a normalized X-axis approach.

### The Toggle Component

A sleek, pill-shaped switch in the top-right of the Analysis View:

* **Distance Mode:** X-axis shows meters or `splinePosition` (0.0 to 1.0). Best for comparing lines/braking points
  regardless of speed.
* **Time Mode:** X-axis shows seconds from start of lap. Best for seeing where time was actually lost.

### Sync-Logic (Vue Composition API)

We'll use a shared `hoverIndex` state. When you scrub the mouse over the **Speed** chart, the **Throttle**, **Brake**,
and **G-Force** charts will all render a vertical synchronized needle.

---

## 3. UI Layout Specification

### A. The Sidebar (Stealth)

Ultra-slim sidebar (64px) that expands on hover. Icons only, using thin-stroke Lucide icons.

* **Live Status:** A glowing green dot at the bottom-left indicating the Ktor server is receiving UDP packets from ACC.

### B. The Analysis Header (Modern/Functional)

* **Lap Info:** `[Lap 14] [1:48.201] [PB: -0.102]`
* **Context Pills:** Small, dark tags for environment data: `[Track: 28°C]` `[Grip: Optimum]` `[Fuel: 12L]`.
* **X-Axis Switch:**
    ```html
    <div class="flex bg-black p-1 rounded-lg border border-white/5">
      <button :class="mode === 'dist' ? 'bg-blue-600' : ''">Distance</button>
      <button :class="mode === 'time' ? 'bg-blue-600' : ''">Time</button>
    </div>
    ```

### C. The Chart Stack

Stacked vertically to allow for easy comparison of peaks and troughs.

1. **Delta Bar (Top):** A thin (10px) bar showing +/- time vs. the reference lap.
2. **Speed (Main):** The largest chart.
3. **Inputs (Combined):** Throttle (Green line) and Brake (Red area fill) on the same chart to see overlapping (trail
   braking).
4. **Gear:** A step-line chart at the bottom.

---

## 4. Vue Component Structure (Typescript)

```typescript
// Proposed Domain Model for the UI
interface TelemetryFrame {
    distance: number; // Meters
    time: number;     // Seconds
    speed: number;
    throttle: number;
    brake: number;
    gear: number;
}

// Composable for syncing charts
const useTelemetrySync = () => {
    const activeX = ref(0); // This is either 'distance' or 'time' value
    const mode = ref<'distance' | 'time'>('distance');

    return {activeX, mode};
}
```

---

## 5. Modern "Functional" Touches

* **Mini-Map Overlay:** A small SVG track map in the corner. As you scrub the telemetry chart, a dot moves along the
  track map to show *exactly* where that gear change or braking spike happened.
* **Empty States:** If no lap is selected, show a high-quality wireframe silhouette of a GT3 car with a "Select a lap to
  begin analysis" prompt.
* **Zero-Jitter Text:** All lap times and live telemetry values use a **Monospace font** (`JetBrains Mono`) so the UI
  doesn't "wiggle" as numbers change rapidly.
