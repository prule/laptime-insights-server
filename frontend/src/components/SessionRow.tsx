import { useNavigate } from "react-router-dom";
import type { SessionResource } from "../api/types";
import { useFeatureEnabled } from "../providers/FeaturesProvider";
import { Badge } from "./ui/Badge";
import { formatDate, formatDrivingTime, formatTime } from "../lib/format";

/**
 * Single row in a sessions table. Navigation prefers the HATEOAS `self` link
 * provided by the backend; we treat it as opaque, only extracting the trailing
 * UID segment so we can hand it to React Router. This means the backend can
 * change its path scheme and the frontend follows.
 *
 * Falls back to a non-clickable row when the `sessions` feature is disabled —
 * the row still renders so summary contexts (e.g. Overview "Recent sessions")
 * stay useful, but the click target is suppressed so the user can't navigate
 * to a screen they shouldn't see.
 */
export function SessionRow({ session }: { session: SessionResource }) {
  const navigate = useNavigate();
  const sessionsEnabled = useFeatureEnabled("sessions");
  const selfUrl = session._links.self ?? "";
  const uidFromLink = selfUrl ? selfUrl.split("/").filter(Boolean).pop() : session.uid;

  return (
    <button
      type="button"
      onClick={sessionsEnabled ? () => navigate(`/sessions/${uidFromLink}`) : undefined}
      disabled={!sessionsEnabled}
      className={[
        "grid w-full grid-cols-[90px_1fr_120px_90px_140px] items-center gap-3 rounded-md p-[10px] text-left transition-colors",
        sessionsEnabled ? "hover:bg-surface-hover" : "cursor-default",
      ].join(" ")}
    >
      <div className="font-mono text-[11px] text-text-muted">
        {formatDate(session.startedAt)}
        <br />
        <span className="text-text-dim">{formatTime(session.startedAt)}</span>
      </div>
      <div>
        <div className="font-sans text-[13px] font-medium text-text">{session.track ?? "Unknown"}</div>
        <div className="font-sans text-[11px] text-text-muted">{session.car ?? "Unknown"}</div>
      </div>
      <div className="flex items-center gap-2">
        <Badge type={session.sessionType} />
      </div>
      <div className="font-mono text-[11px] uppercase tracking-[0.05em] text-text-muted">
        {session.simulator}
      </div>
      <div className="font-mono text-[12px] text-text-muted">
        {formatDrivingTime(session.drivingTimeMs)}
      </div>
    </button>
  );
}
