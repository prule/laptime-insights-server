import { useMemo, useRef } from "react";
import type { TelemetrySample } from "../../api/types";

export interface TrackMapSeries {
  samples: TelemetrySample[];
  color: string;
  label: string;
}

export interface TrackMapProps {
  /** Same series shape as TelemetryTrace — worldPosX/worldPosY used for the track outline. */
  series: TrackMapSeries[];
  /** Controlled crosshair position (0–1 splinePosition). */
  hoveredPosition?: number | null;
  /** Fires when the pointer moves over the map. Passes null on mouse leave. */
  onHover?: (position: number | null) => void;
  size?: number;
}

/**
 * 2-D track map rendered from worldPosX/worldPosY telemetry coordinates.
 *
 * The track outline is drawn from the first series' samples (both laps share
 * the same track layout so their outlines are identical up to minor variation).
 * A colored dot per lap shows where each car was at the hovered splinePosition.
 *
 * Coordinates are normalised into a square viewBox so the map always fills
 * the container regardless of track aspect ratio.
 */
export function TrackMap({ series, hoveredPosition, onHover, size = 240 }: TrackMapProps) {
  const svgRef = useRef<SVGSVGElement>(null);

  const { outline, bounds } = useMemo(() => {
    const src = series[0]?.samples ?? [];
    if (src.length === 0) return { outline: "", bounds: null };

    const xs = src.map((s) => s.worldPosX);
    const ys = src.map((s) => s.worldPosY);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);

    return {
      outline: src
        .map((s, i) => {
          const nx = ((s.worldPosX - minX) / (maxX - minX || 1)) * 1000;
          const ny = ((s.worldPosY - minY) / (maxY - minY || 1)) * 1000;
          return `${i === 0 ? "M" : "L"}${nx.toFixed(1)},${ny.toFixed(1)}`;
        })
        .join(" ") + " Z",
      bounds: { minX, maxX, minY, maxY },
    };
  }, [series]);

  /** Map world coords → normalised SVG space [0, 1000]. */
  const normalise = (x: number, y: number) => {
    if (!bounds) return { nx: 0, ny: 0 };
    const nx = ((x - bounds.minX) / (bounds.maxX - bounds.minX || 1)) * 1000;
    const ny = ((y - bounds.minY) / (bounds.maxY - bounds.minY || 1)) * 1000;
    return { nx, ny };
  };

  /** Find the sample in a series nearest to a given splinePosition. */
  const nearestSample = (samples: TelemetrySample[], pos: number): TelemetrySample | null => {
    if (samples.length === 0) return null;
    return samples.reduce((best, s) =>
      Math.abs(s.splinePosition - pos) < Math.abs(best.splinePosition - pos) ? s : best,
    );
  };

  const handleMouseMove = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!onHover || !svgRef.current || !bounds) return;
    // Convert pointer to SVG coords, then find the nearest sample by world position.
    const rect = svgRef.current.getBoundingClientRect();
    const svgX = ((e.clientX - rect.left) / rect.width) * 1000;
    const svgY = ((e.clientY - rect.top) / rect.height) * 1000;
    const src = series[0]?.samples;
    if (!src || src.length === 0) return;
    const nearest = src.reduce((best, s) => {
      const { nx: bx, ny: by } = normalise(best.worldPosX, best.worldPosY);
      const { nx: cx, ny: cy } = normalise(s.worldPosX, s.worldPosY);
      const dBest = (bx - svgX) ** 2 + (by - svgY) ** 2;
      const dCurr = (cx - svgX) ** 2 + (cy - svgY) ** 2;
      return dCurr < dBest ? s : best;
    });
    onHover(nearest.splinePosition);
  };

  if (!outline) return null;

  return (
    <div className="flex flex-col items-center gap-2">
      <svg
        ref={svgRef}
        viewBox="0 0 1000 1000"
        className="cursor-crosshair rounded"
        style={{ width: size, height: size }}
        onMouseMove={handleMouseMove}
        onMouseLeave={() => onHover?.(null)}
      >
        {/* Track outline — use first series as the reference layout */}
        <path
          d={outline}
          fill="none"
          stroke="rgba(255,255,255,0.15)"
          strokeWidth={8}
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Lap position dots */}
        {hoveredPosition != null &&
          series.map((s) => {
            const sample = nearestSample(s.samples, hoveredPosition);
            if (!sample) return null;
            const { nx, ny } = normalise(sample.worldPosX, sample.worldPosY);
            return (
              <g key={s.label}>
                {/* Outer ring */}
                <circle cx={nx} cy={ny} r={18} fill="none" stroke={s.color} strokeWidth={4} opacity={0.6} />
                {/* Filled dot */}
                <circle cx={nx} cy={ny} r={10} fill={s.color} />
              </g>
            );
          })}
        {/* Start/finish marker */}
        {series[0]?.samples[0] && (() => {
          const s0 = series[0].samples[0]!;
          const { nx, ny } = normalise(s0.worldPosX, s0.worldPosY);
          return (
            <rect
              x={nx - 6}
              y={ny - 12}
              width={12}
              height={24}
              fill="rgba(255,255,255,0.6)"
              rx={2}
            />
          );
        })()}
      </svg>
      {/* Legend */}
      <div className="flex gap-4 font-mono text-[10px] text-text-muted">
        {series.map((s) => (
          <span key={s.label} className="flex items-center gap-1">
            <span
              className="inline-block h-2 w-2 rounded-full"
              style={{ background: s.color }}
            />
            {s.label}
          </span>
        ))}
      </div>
    </div>
  );
}
