import { useMemo } from "react";
import { useLaps, useSessions } from "../api/queries";
import type { LapResource, SessionResource } from "../api/types";
import { formatLapTime } from "../lib/format";

export interface LapPickerProps {
  label: string;
  /** Constrain the lap list to a single track to keep comparisons sensible. */
  trackFilter?: string;
  selectedUid: string | undefined;
  onSelect: (uid: string | undefined) => void;
}

/**
 * Single dropdown that lists laps grouped by their owning session. Optional
 * track filter — comparing two laps from different tracks is supported by
 * the backend but the spline-position alignment is meaningless, so the
 * caller usually pins to one track.
 */
export function LapPicker({ label, trackFilter, selectedUid, onSelect }: LapPickerProps) {
  const sessionsQuery = useSessions({ track: trackFilter, size: 200, sort: "startedAt:DESC" });
  const lapsQuery = useLaps({
    size: 1000,
    sort: "lapTime:ASC",
    track: trackFilter,
    validLap: true,
  });

  const sessionsByUid = useMemo(() => {
    const m = new Map<string, SessionResource>();
    for (const s of sessionsQuery.data?.items ?? []) m.set(s.uid, s);
    return m;
  }, [sessionsQuery.data]);

  const grouped = useMemo(() => {
    const laps = lapsQuery.data?.items ?? [];
    const groups = new Map<string, LapResource[]>();
    for (const lap of laps) {
      const list = groups.get(lap.sessionUid) ?? [];
      list.push(lap);
      groups.set(lap.sessionUid, list);
    }
    // Stable order: most recent session first.
    return Array.from(groups.entries()).sort(([uidA], [uidB]) => {
      const a = sessionsByUid.get(uidA)?.startedAt ?? "";
      const b = sessionsByUid.get(uidB)?.startedAt ?? "";
      return b.localeCompare(a);
    });
  }, [lapsQuery.data, sessionsByUid]);

  return (
    <label className="flex flex-col gap-1">
      <span className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">{label}</span>
      <select
        value={selectedUid ?? ""}
        onChange={(e) => onSelect(e.target.value || undefined)}
        className="rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none focus:border-cyan/40"
      >
        <option value="">— select a lap —</option>
        {grouped.map(([sessionUid, laps]) => {
          const session = sessionsByUid.get(sessionUid);
          const labelText = session
            ? `${session.track ?? "?"} · ${session.car ?? "?"} · ${(session.startedAt ?? "").slice(0, 10)}`
            : sessionUid;
          return (
            <optgroup key={sessionUid} label={labelText}>
              {laps.map((lap) => (
                <option key={lap.uid} value={lap.uid}>
                  Lap {lap.lapNumber} · {formatLapTime(lap.lapTime)}
                  {lap.personalBest ? " · PB" : ""}
                </option>
              ))}
            </optgroup>
          );
        })}
      </select>
    </label>
  );
}
