/**
 * Bubble layout where each circle represents a track and its area is roughly
 * proportional to the lap count recorded at that track over the active time
 * range. Tracks with zero laps in range still render as faded outlines so the
 * gap is visible at a glance — that's the "what haven't I practiced" signal.
 *
 * Layout uses native flex-wrap rather than a packing algorithm: it's simpler,
 * accessible, and reflows on container resize without manual SVG bookkeeping.
 */

export interface TrackPracticeDatum {
  track: string;
  count: number;
}

const MIN_DIAMETER = 30;
const MAX_DIAMETER = 96;

export function TrackPracticeChart({ items }: { items: TrackPracticeDatum[] }) {
  if (items.length === 0) return null;
  // sqrt scaling makes *area* (not diameter) proportional to count, which is
  // how the eye actually compares circles. Tracks with zero laps clamp to the
  // minimum size so the placeholder bubbles don't disappear.
  const maxCount = Math.max(...items.map((i) => i.count), 1);
  const sorted = [...items].sort((a, b) => b.count - a.count);

  return (
    <div className="flex flex-wrap items-end justify-center gap-x-4 gap-y-3 py-2">
      {sorted.map((it) => {
        const ratio = Math.sqrt(it.count / maxCount);
        const diameter =
          it.count === 0
            ? MIN_DIAMETER
            : Math.round(MIN_DIAMETER + ratio * (MAX_DIAMETER - MIN_DIAMETER));
        const isEmpty = it.count === 0;
        return (
          <div
            key={it.track}
            className="flex flex-col items-center"
            style={{ width: Math.max(diameter, 80) }}
          >
            <div
              role="img"
              aria-label={`${it.track}: ${it.count} laps`}
              title={`${it.track} — ${it.count} lap${it.count === 1 ? "" : "s"}`}
              className={[
                "flex items-center justify-center rounded-full border font-mono transition-colors",
                isEmpty
                  ? "border-border border-dashed text-text-dim"
                  : "border-cyan/40 bg-gradient-to-br from-cyan/20 to-cyan/5 text-cyan",
              ].join(" ")}
              style={{
                width: diameter,
                height: diameter,
                fontSize: Math.max(10, Math.round(diameter / 5)),
              }}
            >
              {isEmpty ? "—" : it.count}
            </div>
            <div
              className="mt-1 max-w-[120px] truncate text-center font-sans text-[11px] text-text-muted"
              title={it.track}
            >
              {it.track}
            </div>
          </div>
        );
      })}
    </div>
  );
}
