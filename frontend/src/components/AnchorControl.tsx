import { useState } from "react";
import { useSession } from "../api/queries";
import type { LapResource, SessionResource } from "../api/types";
import { LapLeaderboard } from "./LapLeaderboard";
import { Modal } from "./ui/Modal";
import { formatDate, formatLapTime } from "../lib/format";

export interface AnchorControlProps {
  /** Comparison track — the anchor must be a lap on this track. */
  track: string | undefined;
  /** The resolved anchor lap (explicit selection or seeded default). */
  anchorLap: LapResource | undefined;
  /** Car to seed the change-modal's "Same car" filter from. */
  seedCar?: string;
  /** Session "This session" scope refers to inside the change modal. */
  scopeSession?: SessionResource;
  /** True when the anchor is the seeded default rather than an explicit pick. */
  isDefault: boolean;
  accentColor: string;
  onChange: (lap: LapResource) => void;
}

/**
 * Anchor lap display + "change" modal. The anchor is the reference point of the
 * comparison — seeded to the player's fastest lap in the latest session (or the
 * session best) and changeable to any other lap on the comparison track via the
 * same ranked `LapLeaderboard` used for the challenger.
 */
export function AnchorControl({
  track,
  anchorLap,
  seedCar,
  scopeSession,
  isDefault,
  accentColor,
  onChange,
}: AnchorControlProps) {
  const [open, setOpen] = useState(false);
  const sessionQuery = useSession(anchorLap?.sessionUid);
  const session = sessionQuery.data;

  const summary = anchorLap
    ? `${formatLapTime(anchorLap.lapTime)} · ${anchorLap.car ?? "?"} · ${formatDate(session?.startedAt)}`
    : "— no anchor —";

  return (
    <div className="flex flex-col gap-1">
      <span className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
        Anchor {isDefault && anchorLap && <span className="text-text-dim">· auto</span>}
      </span>
      <div className="flex items-stretch gap-1">
        <button
          onClick={() => setOpen(true)}
          disabled={!track}
          className="min-w-[280px] flex-1 rounded border border-border bg-surface px-3 py-2 text-left font-sans text-[13px] text-text hover:border-cyan/40 disabled:opacity-40"
        >
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 flex-shrink-0 rounded-full" style={{ background: accentColor }} />
            <span className={anchorLap ? "" : "text-text-muted"}>{summary}</span>
            {anchorLap?.playerLap === true && <span className="ml-auto text-[10px] text-cyan">you</span>}
          </div>
        </button>
      </div>

      {track && (
        <Modal open={open} onClose={() => setOpen(false)} title="Change anchor lap">
          <LapLeaderboard
            track={track}
            seedCar={seedCar}
            scopeSession={scopeSession}
            defaultDriver={anchorLap?.playerLap === true ? "me" : "field"}
            selectedLapUid={anchorLap?.uid}
            onPick={(lap) => {
              onChange(lap);
              setOpen(false);
            }}
          />
        </Modal>
      )}
    </div>
  );
}
