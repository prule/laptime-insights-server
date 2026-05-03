export interface BarChartProps {
  data: { label: string; value: number }[];
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
    <div className="flex items-end gap-[6px]" style={{ height }}>
      {data.map((d, i) => (
        <div key={`${d.label}-${i}`} className="flex h-full flex-1 flex-col items-center gap-1">
          <div className="flex w-full flex-1 items-end">
            <div
              className={`w-full rounded-t-[3px] bg-gradient-to-b ${FILL[colorClass]} transition-[height] duration-500`}
              style={{ height: `${Math.max(4, (d.value / max) * 100)}%` }}
            />
          </div>
          <div className="font-mono text-[9px] text-text-dim whitespace-nowrap">{d.label}</div>
        </div>
      ))}
    </div>
  );
}
