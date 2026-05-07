import { useState } from "react";
import { useLaps, useSessionOptions } from "../api/queries";
import { ErrorState, LoadingState, EmptyState } from "./ui/States";
import { FilterSelect } from "./ui/FilterSelect";
import { LapTable } from "./LapTable";

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
          <LapTable
            laps={items}
            onRowClick={(lap) => onPick(lap.uid)}
            isRowDisabled={(lap) => disabledLapUid === lap.uid}
            disabledTitle="Already chosen as the other lap"
          />
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
