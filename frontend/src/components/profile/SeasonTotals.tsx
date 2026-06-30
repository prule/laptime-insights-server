import type { Totals } from "../../api/profile";

interface Tot {
  n: string;
  label: string;
  /** Token-derived accent for the top hairline. */
  accent: string;
}

/** Totals strip: laps, distance, seat time, sessions, active days, circuits. */
export function SeasonTotals({ totals }: { totals: Totals }) {
  const cells: Tot[] = [
    { n: totals.laps.toLocaleString(), label: "Laps", accent: "var(--color-cyan, #00d4ff)" },
    { n: `${totals.distanceKm.toLocaleString()} km`, label: "Distance", accent: "#e8212a" },
    { n: `${totals.hours} h`, label: "Seat time", accent: "#eab308" },
    { n: String(totals.sessions), label: "Sessions", accent: "#22c55e" },
    { n: String(totals.daysActive), label: "Active days", accent: "#f97316" },
    { n: String(totals.tracks), label: "Circuits", accent: "#a855f7" },
  ];

  return (
    <div className="grid grid-cols-6 gap-3">
      {cells.map((c) => (
        <div
          key={c.label}
          className="relative overflow-hidden rounded-lg border border-border bg-surface p-4"
        >
          <span
            className="absolute left-0 right-0 top-0 h-[2px] opacity-70"
            style={{ background: `linear-gradient(90deg, ${c.accent}, transparent)` }}
          />
          <div className="font-mono text-2xl font-bold tracking-[-0.02em] text-text">{c.n}</div>
          <div className="mt-1 font-sans text-[10px] uppercase tracking-[0.08em] text-text-muted">
            {c.label}
          </div>
        </div>
      ))}
    </div>
  );
}
