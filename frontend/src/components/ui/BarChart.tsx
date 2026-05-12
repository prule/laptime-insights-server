export interface BarChartProps {
  /**
   * One entry per bar. `label` renders below the bar (kept terse to fit narrow columns);
   * `title`, when supplied, becomes the native hover tooltip — use it to expose the full date
   * or any extra context that doesn't fit in the visible label.
   */
  data: { label: string; value: number; title?: string }[];
  /** Tailwind color class root (e.g. `cyan`, `accent`). */
  colorClass?: "cyan" | "accent" | "warn" | "ok";
  height?: number;
}

const FILL: Record<NonNullable<BarChartProps["colorClass"]>, string> = {
  cyan: "from-cyan/80 to-cyan/30",
  accent: "from-accent/80 to-accent/30",
  warn: "from-warn/80 to-warn/30",
  ok: "from-ok/80 to-ok/30",
};

export function BarChart({ data, colorClass = "cyan", height = 80 }: BarChartProps) {
  if (data.length === 0) return null;
  const max = Math.max(...data.map((d) => d.value), 1);
  return (
    <div className="flex w-full items-end gap-[6px] overflow-hidden" style={{ height }}>
      {data.map((d, i) => (
        <div
          key={`${d.label}-${i}`}
          title={d.title}
          // min-w-0 lets the column shrink narrower than its label width — needed in 3-column card
          // layouts where bars get crowded. The parent's overflow-hidden clips any genuine
          // overflow at the chart edge. `title` surfaces the full context on hover.
          className="flex h-full min-w-0 flex-1 cursor-default flex-col items-center gap-1"
        >
          <div className="flex w-full flex-1 items-end">
            <div
              className={`w-full rounded-t-[3px] bg-gradient-to-b ${FILL[colorClass]} transition-[height] duration-500`}
              style={{ height: `${Math.max(4, (d.value / max) * 100)}%` }}
            />
          </div>
          <div className="w-full truncate text-center font-mono text-[9px] text-text-dim">
            {d.label}
          </div>
        </div>
      ))}
    </div>
  );
}
