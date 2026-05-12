import { useNavigate } from "react-router-dom";
import type { LapResource } from "../api/types";
import { formatDate, formatLapTime } from "../lib/format";

/**
 * Dashboard table — one row per track, showing the player's all-time fastest valid lap.
 * Source: `/api/1/laps?playerLap=true&validLap=true&allTimeBest=true&sort=track:ASC`.
 *
 * Each row navigates to the owning session's detail page on click when `lap._links.session`
 * is present (i.e. the backend exposed the rel because the `sessions` feature is on).
 */
export function AllTimeBestTable({ laps }: { laps: LapResource[] }) {
  const navigate = useNavigate();

  if (laps.length === 0) {
    return (
      <div className="font-sans text-[12px] text-text-muted">
        No valid player laps recorded yet.
      </div>
    );
  }
  return (
    <div>
      <div className="grid grid-cols-[1fr_140px_140px_120px] gap-3 border-b border-border pb-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted">
        <div>Track</div>
        <div>Best lap</div>
        <div>Car</div>
        <div className="text-right">Recorded</div>
      </div>
      <div className="flex flex-col">
        {laps.map((l) => {
          const canNavigate = !!l._links.session;
          return (
          <button
            key={l.uid}
            type="button"
            onClick={canNavigate ? () => navigate(`/sessions/${l.sessionUid}`) : undefined}
            disabled={!canNavigate}
            className={[
              "grid grid-cols-[1fr_140px_140px_120px] items-center gap-3 border-b border-border/50 py-2 text-left text-[13px] transition-colors last:border-b-0",
              canNavigate ? "hover:bg-surface-hover" : "cursor-default",
            ].join(" ")}
          >
            <div className="font-sans font-medium text-text">{l.track ?? "Unknown"}</div>
            <div className="font-mono font-bold text-ok">{formatLapTime(l.lapTime)}</div>
            <div className="font-sans text-text-muted">{l.car ?? "—"}</div>
            <div className="text-right font-mono text-[11px] text-text-muted">
              {formatDate(l.recordedAt)}
            </div>
          </button>
          );
        })}
      </div>
    </div>
  );
}
