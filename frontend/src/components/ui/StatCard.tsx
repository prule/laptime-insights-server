import { type ReactNode } from "react";

export interface StatCardProps {
  label: string;
  value: ReactNode;
  sub?: ReactNode;
  /** Tailwind color token like `accent`, `cyan`, `ok`, `warn`. */
  accent?: "accent" | "cyan" | "ok" | "warn" | "orange" | "muted";
  small?: boolean;
}

const ACCENT_BORDER: Record<NonNullable<StatCardProps["accent"]>, string> = {
  accent: "from-accent",
  cyan: "from-cyan",
  ok: "from-ok",
  warn: "from-warn",
  orange: "from-orange",
  muted: "from-text-muted",
};

export function StatCard({ label, value, sub, accent = "cyan", small }: StatCardProps) {
  return (
    <div
      className={[
        "relative overflow-hidden rounded-lg border border-border bg-surface",
        small ? "px-[18px] py-4" : "px-[22px] py-5",
        "flex flex-col gap-1",
      ].join(" ")}
    >
      <div
        className={`absolute inset-x-0 top-0 h-[2px] bg-gradient-to-r ${ACCENT_BORDER[accent]} to-transparent opacity-70`}
      />
      <div className="font-sans text-[11px] font-medium uppercase tracking-[0.08em] text-text-muted">
        {label}
      </div>
      <div
        className={[
          "font-mono font-bold leading-tight tracking-tight text-text",
          small ? "text-[22px]" : "text-[28px]",
        ].join(" ")}
      >
        {value}
      </div>
      {sub && <div className="font-sans text-xs text-text-muted">{sub}</div>}
    </div>
  );
}
