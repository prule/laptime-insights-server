import { formatLapTime } from "../../lib/format";

/**
 * Renders a signed difference between two lap times. Positive deltas are
 * shown in the accent color (slower), negative in green (faster).
 *
 * Pass `referenceMs = null` for the row that *is* the reference — renders an
 * em dash with no sign.
 */
export function Delta({ ms, referenceMs }: { ms: number; referenceMs: number | null }) {
  if (referenceMs == null || ms === referenceMs) {
    return <span className="font-mono text-xs text-text-dim">—</span>;
  }
  const diff = ms - referenceMs;
  const sign = diff > 0 ? "+" : "-";
  const colorClass = diff > 0 ? "text-accent" : "text-ok";
  return (
    <span className={`font-mono text-xs font-semibold ${colorClass}`}>
      {sign}
      {formatLapTime(Math.abs(diff)).replace(/^0:/, "")}
    </span>
  );
}
