import type { PerTrack, ProfileMeta } from "../../api/profile";

const MIN_D = 40;
const MAX_D = 96;

/** Proportional, ranked per-circuit stamps. Bubble area ∝ laps, mirroring the dashboard. */
export function TrackStamps({ perTrack, meta }: { perTrack: PerTrack[]; meta: ProfileMeta }) {
  const maxLaps = Math.max(...perTrack.map((t) => t.laps));
  const seasonYear = meta.season.replace(/\D/g, "");

  return (
    <div className="grid grid-cols-3 gap-[14px]">
      {perTrack.map((t, i) => {
        const d = Math.round(MIN_D + (MAX_D - MIN_D) * Math.sqrt(t.laps / maxLaps));
        const numSize = Math.max(13, Math.round(d * 0.26));
        return (
          <div
            key={t.track}
            className="relative overflow-hidden rounded-lg border border-border bg-surface px-5 py-[18px]"
          >
            <div className="absolute left-4 top-3 -rotate-12 rounded-full border border-border-hover px-[7px] py-[3px] font-mono text-[8px] tracking-[0.12em] text-text-dim opacity-70">
              LTI · {seasonYear}
            </div>
            <div className="absolute right-4 top-[14px] font-mono text-[11px] text-text-dim">
              #{i + 1}
            </div>
            <div className="flex h-[112px] items-center justify-center">
              <div
                className="flex items-center justify-center rounded-full"
                style={{
                  width: `${d}px`,
                  height: `${d}px`,
                  background: `radial-gradient(circle at 50% 40%, ${t.accent}33, ${t.accent}14)`,
                  border: `1.5px solid ${t.accent}`,
                  boxShadow: `0 0 16px ${t.accent}40`,
                }}
              >
                <b
                  className="font-mono font-bold leading-none"
                  style={{ fontSize: `${numSize}px`, color: t.accent }}
                >
                  {t.laps}
                </b>
              </div>
            </div>
            <div className="mt-[6px] text-sm font-semibold text-text">{t.track}</div>
            <div className="mt-[2px] font-mono text-xs text-text-muted">{t.laps} laps</div>
          </div>
        );
      })}
    </div>
  );
}
