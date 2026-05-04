import { useMemo, useState } from "react";
import { useLaps, useSessionOptions, useSessions } from "../api/queries";
import type { SessionResource } from "../api/types";
import { ErrorState, LoadingState, EmptyState } from "./ui/States";
import { FilterSelect } from "./ui/FilterSelect";
import { formatDate, formatLapTime } from "../lib/format";

const PAGE_SIZE = 20;

export interface LapBrowserProps {
  /** Pre-selected track (e.g. when picking from a session known to be at Spa). */
  defaultTrack?: string;
  /** Pre-selected car. */
  defaultCar?: string;
  /** Lap UID to disable in the list — typically the lap on the *other* picker. */
  disabledLapUid?: string;
  /** Called when the user picks a lap. */
  onPick: (lapUid: string) => void;
}

/**
 * Filtered + paginated lap list designed for use inside the lap-comparison
 * picker dialog. Mirrors the LapsScreen UX (same filter widgets, same row
 * layout) but in a smaller footprint and with onPick instead of navigation.
 *
 * State is purely local — modal pickers shouldn't pollute the URL.
 */
export function LapBrowser({ defaultTrack, defaultCar, disabledLapUid, onPick }: LapBrowserProps) {
  const optionsQuery = useSessionOptions();
  const [track, setTrack] = useState<string | undefined>(defaultTrack);
  const [car, setCar] = useState<string | undefined>(defaultCar);
  const [validOnly, setValidOnly] = useState(true);
  const [pbOnly, setPbOnly] = useState(false);
  const [page, setPage] = useState(1);

  // Fetch all sessions so we can show track/car/date next to each lap.
  const sessionsQuery = useSessions({ size: 500, sort: "startedAt:DESC" });
  const sessionsByUid = useMemo(() => {
    const m = new Map<string, SessionResource>();
    for (const s of sessionsQuery.data?.items ?? []) m.set(s.uid, s);
    return m;
  }, [sessionsQuery.data]);

  const lapsQuery = useLaps({
    track,
    car,
    page,
    size: PAGE_SIZE,
    sort: "lapTime:ASC",
    validLap: validOnly ? true : undefined,
    personalBest: pbOnly ? true : undefined,
  });

  const onFilterChange = (fn: () => void) => {
    fn();
    setPage(1);
  };

  const items = lapsQuery.data?.items ?? [];

  return (
    <div className="flex flex-col gap-4">
      <div className="flex flex-wrap items-end gap-3">
        <FilterSelect
          label="Track"
          value={track}
          options={optionsQuery.data?.tracks ?? []}
          onChange={(v) => onFilterChange(() => setTrack(v))}
        />
        <FilterSelect
          label="Car"
          value={car}
          options={optionsQuery.data?.cars ?? []}
          onChange={(v) => onFilterChange(() => setCar(v))}
        />
        <Toggle label="Valid only" value={validOnly} onChange={(v) => onFilterChange(() => setValidOnly(v))} />
        <Toggle label="Personal bests" value={pbOnly} onChange={(v) => onFilterChange(() => setPbOnly(v))} />
      </div>

      <div className="font-mono text-[11px] text-text-muted">
        {lapsQuery.data ? `${lapsQuery.data.total} match · page ${page}` : "—"}
      </div>

      {lapsQuery.isLoading && <LoadingState />}
      {lapsQuery.isError && <ErrorState error={lapsQuery.error} onRetry={() => lapsQuery.refetch()} />}
      {lapsQuery.data && items.length === 0 && (
        <EmptyState title="No laps match" description="Loosen the filters." />
      )}
      {items.length > 0 && (
        <>
          <div className="overflow-hidden rounded border border-border">
            <div className="grid grid-cols-[60px_120px_1fr_1fr_90px_90px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
              <div>#</div>
              <div>Lap time</div>
              <div>Track</div>
              <div>Car</div>
              <div>Date</div>
              <div>Status</div>
            </div>
            {items.map((lap, i) => {
              const session = sessionsByUid.get(lap.sessionUid);
              const disabled = disabledLapUid === lap.uid;
              return (
                <button
                  key={lap.uid}
                  onClick={() => !disabled && onPick(lap.uid)}
                  disabled={disabled}
                  title={disabled ? "Already chosen as the other lap" : "Pick this lap"}
                  className={[
                    "grid w-full grid-cols-[60px_120px_1fr_1fr_90px_90px] items-center gap-3 border-b border-border/40 px-3 py-2 text-left last:border-b-0",
                    disabled
                      ? "cursor-not-allowed opacity-30"
                      : "hover:bg-surface-hover",
                  ].join(" ")}
                >
                  <div className="font-mono text-xs text-text-muted">{(page - 1) * PAGE_SIZE + i + 1}</div>
                  <div className={`font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`}>
                    {formatLapTime(lap.lapTime)}
                  </div>
                  <div className="truncate font-sans text-[13px] text-text">
                    {session?.track ?? <span className="text-text-dim">unknown</span>}
                  </div>
                  <div className="truncate font-sans text-[12px] text-text-muted">
                    {lap.car ?? session?.car ?? <span className="text-text-dim">unknown</span>}
                  </div>
                  <div className="font-mono text-xs text-text-muted">{formatDate(session?.startedAt)}</div>
                  <div className="font-mono text-[11px]">
                    {lap.personalBest && <span className="text-ok">PB</span>}
                    {!lap.valid && <span className="text-accent">INVAL</span>}
                  </div>
                </button>
              );
            })}
          </div>
          <div className="flex items-center gap-2">
            <button
              disabled={page === 1}
              onClick={() => setPage((p) => Math.max(1, p - 1))}
              className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
            >
              Prev
            </button>
            <button
              disabled={!lapsQuery.data || page * PAGE_SIZE >= lapsQuery.data.total}
              onClick={() => setPage((p) => p + 1)}
              className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
            >
              Next
            </button>
          </div>
        </>
      )}
    </div>
  );
}

function Toggle({
  label,
  value,
  onChange,
}: {
  label: string;
  value: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <button
      onClick={() => onChange(!value)}
      className={[
        "self-end rounded border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors",
        value ? "border-cyan/30 bg-cyan/10 text-cyan" : "border-border text-text-muted hover:bg-surface-hover",
      ].join(" ")}
    >
      {value ? "✓" : "○"} {label}
    </button>
  );
}
