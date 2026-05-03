/**
 * Session-type badge. The Ktor backend stores `sessionType` as a free-form
 * string (e.g. "Race", "Qualifying", "Practice"), so we normalize for display.
 */
const STYLES: Record<string, { label: string; className: string }> = {
  RACE: { label: "RACE", className: "bg-accent/15 text-accent" },
  QUALIFYING: { label: "QUALI", className: "bg-warn/15 text-warn" },
  QUALI: { label: "QUALI", className: "bg-warn/15 text-warn" },
  PRACTICE: { label: "PRACTICE", className: "bg-text-muted/20 text-text-muted" },
};

export function Badge({ type }: { type: string | null | undefined }) {
  const key = (type ?? "").toUpperCase();
  const style = STYLES[key] ?? STYLES.PRACTICE!;
  return (
    <span
      className={`inline-flex items-center rounded font-mono text-[10px] font-semibold uppercase tracking-[0.1em] px-[7px] py-[3px] ${style.className}`}
    >
      {style.label}
    </span>
  );
}
