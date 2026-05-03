import type { TelemetrySample } from "../../api/types";

/**
 * Strip showing where the two laps were in different gears at the same point
 * on track. Per the spec: "Gear Variance: Highlight segments where the gear
 * index differs between Lap A and Lap B."
 */
export function GearMismatchStrip({
  lap1,
  lap2,
  height = 32,
}: {
  lap1: TelemetrySample[];
  lap2: TelemetrySample[];
  height?: number;
}) {
  if (lap1.length === 0 || lap2.length === 0) return null;
  const buckets = 100;
  const g1 = resampleGear(lap1, buckets);
  const g2 = resampleGear(lap2, buckets);
  const width = 1000;
  const cellW = width / buckets;

  return (
    <svg
      viewBox={`0 0 ${width} ${height}`}
      preserveAspectRatio="none"
      className="block w-full"
      style={{ height }}
    >
      {g1.map((gear1, i) => {
        const gear2 = g2[i] ?? gear1;
        const mismatch = gear1 !== gear2;
        if (!mismatch) return null;
        return (
          <rect
            key={i}
            x={i * cellW}
            y={0}
            width={cellW}
            height={height}
            fill="rgba(232,33,42,0.55)"
          />
        );
      })}
    </svg>
  );
}

function resampleGear(samples: TelemetrySample[], buckets: number): number[] {
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
    out[i] = sorted[cursor]!.gear;
  }
  return out;
}
