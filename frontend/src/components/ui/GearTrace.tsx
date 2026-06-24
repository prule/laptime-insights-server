import { useRef } from "react";
import type { TelemetrySample } from "../../api/types";

export interface GearTraceProps {
  /** Series in render order (drawn last = on top). */
  series: { samples: TelemetrySample[]; color: string; label: string }[];
  height?: number;
  /** Controlled crosshair position (0–1 splinePosition). */
  hoveredPosition?: number | null;
  /** Fires when the pointer moves over the chart; parent lifts state so panels sync. */
  onHover?: (position: number | null) => void;
}

/**
 * Continuous gear trace for two laps, plotted against splinePosition (0 → 1).
 *
 * Unlike the speed trace, gear is a discrete integer, so each lap is drawn as a
 * stepped line (hold the gear, step vertically when it changes) over an integer
 * Y axis. Track positions where the two laps are in different gears are shaded
 * behind the traces, preserving the old "gear mismatch" insight while now also
 * showing *what* gear each car is in.
 *
 * Shares the Compare screen's synchronized crosshair via hoveredPosition / onHover.
 */
export function GearTrace({ series, height = 140, hoveredPosition, onHover }: GearTraceProps) {
  const svgRef = useRef<SVGSVGElement>(null);

  const allGears = series.flatMap((s) => s.samples.map((sample) => sample.gear));
  if (allGears.length === 0 || series.some((s) => s.samples.length === 0)) return null;

  const minGear = Math.min(...allGears);
  const maxGear = Math.max(...allGears);
  // Pad half a gear top and bottom so the extreme gears aren't clipped to the edge.
  const lo = minGear - 0.5;
  const span = maxGear - minGear + 1; // == (maxGear + 0.5) - (minGear - 0.5)

  const width = 1000; // viewBox width — scales via CSS

  const yFor = (gear: number) => height - ((gear - lo) / span) * height;

  // Stepped path: horizontal hold at the current gear, then a vertical step to the next.
  const pathFor = (samples: TelemetrySample[]) => {
    const sorted = samples.slice().sort((a, b) => a.splinePosition - b.splinePosition);
    let d = "";
    sorted.forEach((sample, i) => {
      const x = sample.splinePosition * width;
      const y = yFor(sample.gear);
      if (i === 0) {
        d += `M${x.toFixed(2)},${y.toFixed(2)}`;
      } else {
        const prevY = yFor(sorted[i - 1]!.gear);
        // hold previous gear to this x, then step to this gear
        d += ` L${x.toFixed(2)},${prevY.toFixed(2)} L${x.toFixed(2)},${y.toFixed(2)}`;
      }
    });
    return d;
  };

  const buckets = 100;
  const cellW = width / buckets;
  const resampled = series.map((s) => resampleGear(s.samples, buckets));
  const mismatches: number[] =
    resampled.length === 2
      ? resampled[0]!.map((g, i) => (g !== resampled[1]![i] ? i : -1)).filter((i) => i >= 0)
      : [];

  const gearLines: number[] = [];
  for (let g = minGear; g <= maxGear; g++) gearLines.push(g);

  const handleMouseMove = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!onHover || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    onHover(Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)));
  };

  // Nearest-sample gear per lap for the crosshair tooltip.
  const crosshairValues =
    hoveredPosition != null
      ? series.map((s) => {
          const nearest = s.samples.reduce((best, sample) =>
            Math.abs(sample.splinePosition - hoveredPosition!) <
            Math.abs(best.splinePosition - hoveredPosition!)
              ? sample
              : best,
          );
          return { color: s.color, label: s.label, gear: nearest.gear };
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
        {/* Mismatch shading, behind everything */}
        {mismatches.map((i) => (
          <rect
            key={`m-${i}`}
            x={i * cellW}
            y={0}
            width={cellW}
            height={height}
            fill="rgba(232,33,42,0.14)"
          />
        ))}
        {/* Integer gear gridlines */}
        {gearLines.map((g) => {
          const y = yFor(g);
          return (
            <line
              key={`g-${g}`}
              x1={0}
              x2={width}
              y1={y}
              y2={y}
              stroke="rgba(255,255,255,0.05)"
              strokeWidth={1}
              vectorEffect="non-scaling-stroke"
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
              {v.label}: {gearLabel(v.gear)}
            </div>
          ))}
        </div>
      )}
      <div className="mt-1 flex justify-between font-mono text-[10px] text-text-dim">
        <span>{gearLabel(minGear)}</span>
        <span>{gearLabel(maxGear)}</span>
      </div>
    </div>
  );
}

/** ACC gear: 0 = neutral, -1 = reverse, 1..N = forward gear number. */
function gearLabel(gear: number): string {
  if (gear === 0) return "N";
  if (gear < 0) return "R";
  return String(gear);
}

function resampleGear(samples: TelemetrySample[], buckets: number): number[] {
  if (samples.length === 0) return [];
  const sorted = samples.slice().sort((a, b) => a.splinePosition - b.splinePosition);
  const out: number[] = new Array(buckets);
  let cursor = 0;
  for (let i = 0; i < buckets; i++) {
    const target = i / (buckets - 1);
    while (cursor < sorted.length - 1 && (sorted[cursor + 1]?.splinePosition ?? 1) < target) {
      cursor++;
    }
    out[i] = sorted[cursor]!.gear;
  }
  return out;
}
