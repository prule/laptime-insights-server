import { useRef } from "react";
import type { TelemetrySample } from "../../api/types";

export interface TelemetryTraceProps {
  /** Series in render order (drawn last = on top). */
  series: { samples: TelemetrySample[]; color: string; label: string }[];
  /** Telemetry field to plot. */
  field: "speedKph";
  height?: number;
  /** Optional unit suffix in tooltip / axis labels. */
  unit?: string;
  /** Optional Y-axis range; auto-fit if omitted. */
  yMin?: number;
  yMax?: number;
  /** Controlled crosshair position (0–1 splinePosition). Renders a vertical
   *  line at that position when set. Driven by the parent's hover state. */
  hoveredPosition?: number | null;
  /** Fires when the pointer moves over the chart. Parent lifts state so all
   *  panels stay in sync. Passes null on mouse leave. */
  onHover?: (position: number | null) => void;
}

/**
 * Continuous trace plotted against splinePosition (0 → 1).
 *
 * Multiple series share the same X axis and Y range so two laps overlay 1:1
 * spatially. Each series renders its own polyline; the parent decides the
 * draw order (typically reference lap drawn first, focus lap last).
 *
 * Supports a synchronized crosshair via hoveredPosition / onHover props so
 * all chart panels on the compare screen track the same track position.
 */
export function TelemetryTrace({
  series,
  field,
  height = 140,
  unit,
  yMin,
  yMax,
  hoveredPosition,
  onHover,
}: TelemetryTraceProps) {
  const svgRef = useRef<SVGSVGElement>(null);

  const all = series.flatMap((s) => s.samples.map((sample) => sample[field]));
  if (all.length === 0) return null;
  const min = yMin ?? Math.min(...all);
  const max = yMax ?? Math.max(...all);
  const span = max - min || 1;

  const width = 1000; // viewBox width — scales via CSS

  const pathFor = (samples: TelemetrySample[]) =>
    samples
      .map((sample, i) => {
        const x = sample.splinePosition * width;
        const y = height - ((sample[field] - min) / span) * height;
        return `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
      })
      .join(" ");

  const handleMouseMove = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!onHover || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    const position = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
    onHover(position);
  };

  // Find the sample value nearest to the crosshair for the tooltip.
  const crosshairValues =
    hoveredPosition != null
      ? series.map((s) => {
          const nearest = s.samples.reduce((best, sample) =>
            Math.abs(sample.splinePosition - hoveredPosition!) <
            Math.abs(best.splinePosition - hoveredPosition!)
              ? sample
              : best,
          );
          return { color: s.color, label: s.label, value: nearest[field] };
        })
      : null;

  return (
    <div className="relative">
      <svg
        ref={svgRef}
        viewBox={`0 0 ${width} ${height}`}
        preserveAspectRatio="none"
        className="block w-full cursor-crosshair"
        style={{ height }}
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHover?.(null)}
      >
        {/* Y axis ticks */}
        {[0, 0.25, 0.5, 0.75, 1].map((t) => {
          const y = height * t;
          return (
            <line
              key={t}
              x1={0}
              x2={width}
              y1={y}
              y2={y}
              stroke="rgba(255,255,255,0.04)"
              strokeWidth={1}
            />
          );
        })}
        {series.map((s, idx) => (
          <path
            key={`${s.label}-${idx}`}
            d={pathFor(s.samples)}
            fill="none"
            stroke={s.color}
            strokeWidth={1.5}
            vectorEffect="non-scaling-stroke"
            opacity={idx === series.length - 1 ? 1 : 0.7}
          />
        ))}
        {/* Crosshair */}
        {hoveredPosition != null && (
          <line
            x1={hoveredPosition * width}
            x2={hoveredPosition * width}
            y1={0}
            y2={height}
            stroke="rgba(255,255,255,0.5)"
            strokeWidth={1}
            vectorEffect="non-scaling-stroke"
          />
        )}
      </svg>
      {/* Crosshair tooltip */}
      {hoveredPosition != null && crosshairValues && (
        <div
          className="pointer-events-none absolute top-1 z-10 rounded bg-surface-1 px-2 py-1 font-mono text-[10px] shadow"
          style={{
            left: `${Math.min(hoveredPosition * 100, 80)}%`,
            transform: "translateX(-50%)",
          }}
        >
          {crosshairValues.map((v) => (
            <div key={v.label} style={{ color: v.color }}>
              {v.label}: {v.value.toFixed(0)}{unit ? ` ${unit}` : ""}
            </div>
          ))}
        </div>
      )}
      <div className="mt-1 flex justify-between font-mono text-[10px] text-text-dim">
        <span>
          {min.toFixed(field === "speedKph" ? 0 : 2)}
          {unit ? ` ${unit}` : ""}
        </span>
        <span>
          {max.toFixed(field === "speedKph" ? 0 : 2)}
          {unit ? ` ${unit}` : ""}
        </span>
      </div>
    </div>
  );
}
