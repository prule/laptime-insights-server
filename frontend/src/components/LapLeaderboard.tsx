import { useMemo, useState } from "react";
import { useLaps, useSessionLaps } from "../api/queries";
import type { LapResource, SessionResource } from "../api/types";
import { ErrorState, LoadingState, EmptyState } from "./ui/States";
import { LapTable } from "./LapTable";

const PAGE_SIZE = 20;

export type Scope = "session" | "all";
export type Driver = "me" | "field";

export interface LapLeaderboardProps {
  /** Comparison axis. The leaderboard only ever lists laps from this track. */
  track: string;
  /** Car to seed the "Same car" filter from (anchor's car). */
  seedCar?: string;
  /** The session that "This session" scope refers to (the seeding session). */
  scopeSession?: SessionResource;
  /**
   * Initial driver toggle. Defaults to "field" — selecting "me" up front shows an empty list when
   * the player's laps aren't identifiable (`playerLap` null across the data), so the caller passes
   * "me" only when it knows the player (e.g. the anchor is a player lap).
   */
  defaultDriver?: Driver;
  /** Lap currently chosen as the anchor — marked + non-selectable here. */
  anchorLapUid?: string;
  /** Lap currently chosen as the challenger — highlighted. */
  selectedLapUid?: string;
  onPick: (lap: LapResource) => void;
}

/**
 * Ranked, same-track leaderboard used to pick the challenger lap (and to change
 * the anchor). Replaces the old flat `LapBrowser`: laps are always scoped to one
 * track and ordered fastest-first, with toggles for scope / driver / same-car so
 * the user can navigate semantically ("my 2nd fastest", "the field's best in the
 * same car") rather than scrolling a flat list.
 *
 * Two data paths share the toggle UI:
 *  - "All sessions" → server-paginated `useLaps({ track, ... })`; rank is the
 *    absolute position across pages.
 *  - "This session" → `useSessionLaps(scopeSession)` (bounded), filtered + sorted
 *    client-side; rank is the position within that session's qualifying laps.
 */
export function LapLeaderboard({
  track,
  seedCar,
  scopeSession,
  defaultDriver = "field",
  anchorLapUid,
  selectedLapUid,
  onPick,
}: LapLeaderboardProps) {
  const [scope, setScope] = useState<Scope>(scopeSession ? "session" : "all");
  const [driver, setDriver] = useState<Driver>(defaultDriver);
  const [sameCar, setSameCar] = useState(true);
  const [page, setPage] = useState(1);

  const car = sameCar ? seedCar : undefined;

  // "All sessions" — server filters + pagination.
  const allQuery = useLaps({
    track,
    car,
    playerLap: driver === "me" ? true : undefined,
    validLap: true,
    sort: "lapTime:ASC",
    page,
    size: PAGE_SIZE,
  });

  // "This session" — fetch the bounded session list once, filter/sort in memory.
  const sessionQuery = useSessionLaps(scope === "session" ? scopeSession : undefined, {
    sort: "lapTime:ASC",
    size: 200,
  });

  const usingSession = scope === "session" && !!scopeSession;

  const sessionRows = useMemo(() => {
    if (!usingSession) return [];
    const laps = sessionQuery.data?.items ?? [];
    return laps
      .filter((l) => l.valid && l.track === track)
      .filter((l) => (driver === "me" ? l.playerLap === true : true))
      .filter((l) => (car ? l.car === car : true))
      .sort((a, b) => a.lapTime - b.lapTime);
  }, [usingSession, sessionQuery.data, track, driver, car]);

  const rows: LapResource[] = useMemo(
    () => (usingSession ? sessionRows : allQuery.data?.items ?? []),
    [usingSession, sessionRows, allQuery.data],
  );

  // rank → lap.uid, so the prefix column can render position without an index.
  const rankByUid = useMemo(() => {
    const map = new Map<string, number>();
    const offset = usingSession ? 0 : (page - 1) * PAGE_SIZE;
    rows.forEach((lap, i) => map.set(lap.uid, offset + i + 1));
    return map;
  }, [rows, usingSession, page]);

  const total = usingSession ? sessionRows.length : allQuery.data?.total ?? 0;
  const isLoading = usingSession ? sessionQuery.isLoading : allQuery.isLoading;
  const isError = usingSession ? sessionQuery.isError : allQuery.isError;

  const onToggle = (fn: () => void) => {
    fn();
    setPage(1);
  };

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-center gap-2">
        {scopeSession && (
          <SegToggle
            options={[
              { value: "session", label: "This session" },
              { value: "all", label: "All sessions" },
            ]}
            value={scope}
            onChange={(v) => onToggle(() => setScope(v as Scope))}
          />
        )}
        <SegToggle
          options={[
            { value: "me", label: "Me" },
            { value: "field", label: "Field" },
          ]}
          value={driver}
          onChange={(v) => onToggle(() => setDriver(v as Driver))}
        />
        <Toggle
          label={seedCar ? `Same car · ${seedCar}` : "Same car"}
          value={sameCar}
          disabled={!seedCar}
          onChange={(v) => onToggle(() => setSameCar(v))}
        />
      </div>

      <div className="font-mono text-[11px] text-text-muted">
        {isLoading ? "—" : `${total} lap${total === 1 ? "" : "s"}${usingSession ? "" : ` · page ${page}`}`}
      </div>

      {isLoading && <LoadingState />}
      {isError && (
        <ErrorState
          error={usingSession ? sessionQuery.error : allQuery.error}
          onRetry={() => (usingSession ? sessionQuery.refetch() : allQuery.refetch())}
        />
      )}
      {!isLoading && !isError && rows.length === 0 && (
        <EmptyState title="No laps match" description="Loosen the filters — try Field or turn off Same car." />
      )}

      {rows.length > 0 && (
        <>
          <LapTable
            laps={rows}
            onRowClick={onPick}
            isRowDisabled={(lap) => anchorLapUid === lap.uid}
            isRowSelected={(lap) => selectedLapUid === lap.uid}
            disabledTitle="Already chosen as the anchor"
            prefixColumn={{
              header: "#",
              width: "44px",
              cell: (lap) => (
                <span className="font-mono text-[11px] text-text-muted">
                  {rankByUid.get(lap.uid) ?? "—"}
                </span>
              ),
            }}
          />
          {!usingSession && (
            <div className="flex items-center gap-2">
              <PagerButton disabled={page === 1} onClick={() => setPage((p) => Math.max(1, p - 1))}>
                Prev
              </PagerButton>
              <PagerButton
                disabled={!allQuery.data || page * PAGE_SIZE >= allQuery.data.total}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </PagerButton>
            </div>
          )}
        </>
      )}
    </div>
  );
}

function SegToggle({
  options,
  value,
  onChange,
}: {
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <div className="inline-flex overflow-hidden rounded border border-border">
      {options.map((opt) => (
        <button
          key={opt.value}
          onClick={() => onChange(opt.value)}
          className={[
            "px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors",
            value === opt.value ? "bg-cyan/10 text-cyan" : "text-text-muted hover:bg-surface-hover",
          ].join(" ")}
        >
          {opt.label}
        </button>
      ))}
    </div>
  );
}

function Toggle({
  label,
  value,
  disabled,
  onChange,
}: {
  label: string;
  value: boolean;
  disabled?: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <button
      onClick={() => onChange(!value)}
      disabled={disabled}
      className={[
        "rounded border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors disabled:opacity-30",
        value ? "border-cyan/30 bg-cyan/10 text-cyan" : "border-border text-text-muted hover:bg-surface-hover",
      ].join(" ")}
    >
      {value ? "✓" : "○"} {label}
    </button>
  );
}

function PagerButton({
  disabled,
  onClick,
  children,
}: {
  disabled: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      disabled={disabled}
      onClick={onClick}
      className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
    >
      {children}
    </button>
  );
}
