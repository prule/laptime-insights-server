/**
 * Compact pill-button group for the global time range. Lives in the Topbar so
 * it's visible from every screen.
 */
import { TIME_RANGE_OPTIONS, useTimeRange } from "../../providers/TimeRangeProvider";

export function TimeRangeSelector() {
  const { range, setRange } = useTimeRange();

  return (
    <div
      className="flex items-center gap-1 rounded-md border border-border bg-surface px-1 py-1"
      role="group"
      aria-label="Time range"
    >
      {TIME_RANGE_OPTIONS.map((opt) => {
        const active = opt.key === range;
        return (
          <button
            key={opt.key}
            type="button"
            data-testid={`time-range-${opt.key}`}
            onClick={() => setRange(opt.key)}
            title={opt.sub}
            aria-pressed={active}
            className={
              "rounded px-2.5 py-1 font-mono text-[11px] tracking-[0.05em] transition-colors " +
              (active
                ? "bg-accent/20 text-accent"
                : "text-text-muted hover:text-text hover:bg-border/40")
            }
          >
            {opt.label}
          </button>
        );
      })}
    </div>
  );
}
