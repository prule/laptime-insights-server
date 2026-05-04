import { useRef } from "react";
import type { TelemetrySample } from "../../api/types";

export interface SpeedDeltaTraceProps {
  lap1: TelemetrySample[];
  lap2: TelemetrySample[];
  height?: number;
  hoveredPosition?: number | null;
  onHover?: (position: number | null) => void;
}

/**
 * Plots `speedKph(lap1) - speedKph(lap2)` resampled at every 1% of track
 * length, per the lap-comparison spec ("Speed Delta: Calculate the difference
 * in KPH at every 1% of the track length"). Positive delta = lap1 faster than
 * lap2 → renders in cyan; negative = lap2 faster → renders in accent (red).
 */
export function SpeedDeltaTrace({ lap1, lap2, height = 100, hoveredPosition, onHover }: SpeedDeltaTraceProps) {
  const svgRef = useRef<SVGSVGElement>(null);

  if (lap1.length === 0 || lap2.length === 0) return null;
  const buckets = 100;
  const series1 = resample(lap1, buckets);
  const series2 = resample(lap2, buckets);
  const deltas = series1.map((v, i) => v - (series2[i] ?? 0));
  const absMax = Math.max(...deltas.map((d) => Math.abs(d)), 1);

  const width = 1000;
  const zeroY = height / 2;

  const path = deltas
    .map((d, i) => {
      const x = (i / (buckets - 1)) * width;
      const y = zeroY - (d / absMax) * (height / 2 - 4);
      return `${i === 0 ? "M" : "L"}${x.toFixed(2)},${y.toFixed(2)}`;
    })
    .join(" ");

  const handleMouseMove = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!onHover || !svgRef.current) return;
    const rect = svgRef.current.getBoundingClientRect();
    onHover(Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width)));
  };

  const crosshairDelta =
    hoveredPosition != null
      ? (deltas[Math.round(hoveredPosition * (buckets - 1))] ?? null)
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
        <line
          x1={0}
          x2={width}
          y1={zeroY}
          y2={zeroY}
          stroke="rgba(255,255,255,0.12)"
          strokeWidth={1}
        />
        <path d={path} fill="none" stroke="#00d4ff" strokeWidth={1.5} vectorEffect="non-scaling-stroke" />
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
      {hoveredPosition != null && crosshairDelta != null && (
        <div
          className="pointer-events-none absolute top-1 z-10 rounded bg-surface-1 px-2 py-1 font-mono text-[10px] shadow"
          style={{
            left: `${Math.min(hoveredPosition * 100, 80)}%`,
            transform: "translateX(-50%)",
          }}
        >
          <span style={{ color: crosshairDelta >= 0 ? "#00d4ff" : "#e8212a" }}>
            {crosshairDelta >= 0 ? "+" : ""}{crosshairDelta.toFixed(0)} kph
          </span>
        </div>
      )}
      <div className="mt-1 flex justify-between font-mono text-[10px] text-text-dim">
        <span className="text-cyan">+{absMax.toFixed(0)} kph (lap 1 faster)</span>
        <span className="text-accent">-{absMax.toFixed(0)} kph (lap 2 faster)</span>
      </div>
    </div>
  );
}

/** Resample `speedKph` to `buckets` evenly-spaced splinePositions via linear interpolation. */
function resample(samples: TelemetrySample[], buckets: number): number[] {
  if (samples.length === 0) return [];
  const sorted = samples.slice().sort((a, b) => a.splinePosition - b.splinePosition);
  const out: number[] = new Array(buckets);
  let cursor = 0;
  for (let i = 0; i < buckets; i++) {
    const target = i / (buckets - 1);
    while (
      cursor < sorted.length - 1 &&
      (sorted[cursor + 1]?.splinePosition ?? 1) < target
    ) {
      cursor++;
    }
    const a = sorted[cursor]!;
    const b = sorted[Math.min(cursor + 1, sorted.length - 1)]!;
    const span = b.splinePosition - a.splinePosition || 1;
    const ratio = (target - a.splinePosition) / span;
    out[i] = a.speedKph + (b.speedKph - a.speedKph) * Math.max(0, Math.min(1, ratio));
  }
  return out;
}
