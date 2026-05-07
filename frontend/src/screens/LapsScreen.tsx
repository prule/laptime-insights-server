import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps, useSessionOptions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { CarFilterBar } from "../components/ui/CarFilterBar";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import { LapTable } from "../components/LapTable";
import { getBool, getInt, getString, useUrlState } from "../hooks/useUrlState";
import { useTimeRange } from "../providers/TimeRangeProvider";

const PAGE_SIZE = 50;

/**
 * Lap-search screen.
 *
 * Filters and pagination live in the URL querystring (e.g.
 * `/laps?track=Monza&validOnly=true&page=2`) — reload-safe and shareable.
 *
 * Includes a "select mode" so users can pick exactly two laps from the
 * filtered, paginated list and jump straight into the compare screen.
 * Selection is local component state — the URL only carries filter state.
 */
export function LapsScreen() {
  const navigate = useNavigate();
  const [params, setParam, setMany] = useUrlState();

  const facets = {
    track: getString(params, "track"),
    car: getString(params, "car"),
    simulator: getString(params, "simulator"),
  };
  const showInvalid = getBool(params, "invalid", false);
  const validOnly = !showInvalid;
  const pbOnly = getBool(params, "pb", false);
  const page = getInt(params, "page", 1);
  const facetsActive = !!(facets.track || facets.car || facets.simulator);

  // Multi-select state for compare. We deliberately keep this in component
  // state, not the URL — selection is a transient pre-action, not a view.
  const [selectMode, setSelectMode] = useState(false);
  const [selected, setSelected] = useState<string[]>([]);

  const { fromIso } = useTimeRange();
  const from = fromIso ?? undefined;

  const optionsQuery = useSessionOptions();

  const lapsQuery = useLaps({
    page,
    size: PAGE_SIZE,
    sort: "lapTime:ASC",
    validLap: validOnly ? true : undefined,
    personalBest: pbOnly ? true : undefined,
    car: facets.car,
    track: facets.track,
    simulator: facets.simulator,
    from,
  });

  const items = lapsQuery.data?.items ?? [];

  const updateFacet = (key: "track" | "car" | "simulator", value: string | undefined) => {
    setMany({ [key]: value, page: undefined });
  };

  const toggleSelect = (lapUid: string) => {
    setSelected((prev) => {
      if (prev.includes(lapUid)) return prev.filter((u) => u !== lapUid);
      // Cap at 2: drop the older selection so the most recent two clicks win.
      if (prev.length >= 2) return [prev[1]!, lapUid];
      return [...prev, lapUid];
    });
  };

  const compareSelected = () => {
    if (selected.length !== 2) return;
    const [lap1, lap2] = selected as [string, string];
    // Track is preserved in URL only when the user picked one — otherwise
    // /compare infers it from the chosen laps.
    const trackParam = facets.track ? `&track=${encodeURIComponent(facets.track)}` : "";
    navigate(`/compare?lap1=${lap1}&lap2=${lap2}${trackParam}`);
  };

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader title="Filter" sub="All filters hit /api/1/laps directly · state is mirrored to the URL" />
        <div className="flex flex-wrap items-end gap-3">
          <FilterSelect
            label="Track"
            value={facets.track}
            options={optionsQuery.data?.tracks ?? []}
            onChange={(v) => updateFacet("track", v)}
          />
          <FilterSelect
            label="Simulator"
            value={facets.simulator}
            options={optionsQuery.data?.simulators ?? []}
            onChange={(v) => updateFacet("simulator", v)}
          />
          <Toggle
            label="Valid only"
            value={validOnly}
            onChange={(v) => setMany({ invalid: v ? undefined : true, page: undefined })}
          />
          <Toggle
            label="Personal bests"
            value={pbOnly}
            onChange={(v) => setMany({ pb: v ? true : undefined, page: undefined })}
          />
          {(facetsActive || pbOnly || !validOnly) && (
            <button
              onClick={() =>
                setMany({
                  track: undefined,
                  car: undefined,
                  simulator: undefined,
                  invalid: undefined,
                  pb: undefined,
                  page: undefined,
                })
              }
              className="self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
            >
              Reset
            </button>
          )}
        </div>
      </Card>

      {(optionsQuery.data?.cars?.length ?? 0) > 1 && (
        <div className="mb-4">
          <CarFilterBar
            cars={(optionsQuery.data?.cars ?? []).map((c) => ({ value: c, label: c }))}
            selected={facets.car ?? null}
            onChange={(v) => updateFacet("car", v ?? undefined)}
          />
        </div>
      )}

      <Card>
        <div className="mb-4 flex items-baseline justify-between">
          <div>
            <div className="font-sans text-sm font-medium text-text">Fastest laps</div>
            <div className="font-sans text-xs text-text-muted">
              {lapsQuery.data ? `${lapsQuery.data.total} match · page ${page}` : "—"}
              {selectMode && (
                <span className="ml-2 text-cyan">
                  · select mode: {selected.length}/2 picked
                </span>
              )}
            </div>
          </div>
          <div className="flex items-center gap-2">
            {selectMode ? (
              <>
                <button
                  onClick={compareSelected}
                  disabled={selected.length !== 2}
                  className="rounded border border-cyan/40 bg-cyan/10 px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-cyan transition-colors disabled:cursor-not-allowed disabled:opacity-30"
                >
                  Compare selected
                </button>
                <button
                  onClick={() => {
                    setSelected([]);
                    setSelectMode(false);
                  }}
                  className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                onClick={() => setSelectMode(true)}
                className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:border-cyan/40 hover:text-cyan"
              >
                Select to compare
              </button>
            )}
          </div>
        </div>
        {lapsQuery.isLoading && <LoadingState />}
        {lapsQuery.isError && (
          <ErrorState error={lapsQuery.error} onRetry={() => lapsQuery.refetch()} />
        )}
        {lapsQuery.data && items.length === 0 && (
          <EmptyState
            title="No laps match"
            description={facetsActive ? "Try clearing the car/track/simulator filters." : "Loosen the toggles or seed the database."}
          />
        )}
        {items.length > 0 && (
          <>
            <LapTable
              laps={items}
              onRowClick={(lap) =>
                selectMode ? toggleSelect(lap.uid) : navigate(`/sessions/${lap.sessionUid}`)
              }
              onSessionClick={(uid) => navigate(`/sessions/${uid}`)}
              isRowSelected={(lap) => selected.includes(lap.uid)}
              prefixColumn={
                selectMode
                  ? {
                      width: "36px",
                      cell: (lap) => (
                        <div className="flex items-center justify-center">
                          <span
                            aria-hidden
                            className={[
                              "flex h-4 w-4 items-center justify-center rounded border font-mono text-[10px]",
                              selected.includes(lap.uid)
                                ? "border-cyan bg-cyan/20 text-cyan"
                                : "border-border text-transparent",
                            ].join(" ")}
                          >
                            ✓
                          </span>
                        </div>
                      ),
                    }
                  : undefined
              }
            />
            <div className="mt-3 flex items-center gap-2">
              <button
                disabled={page === 1}
                onClick={() => setParam("page", page > 2 ? page - 1 : undefined)}
                className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
              >
                Prev
              </button>
              <button
                disabled={!lapsQuery.data || page * PAGE_SIZE >= lapsQuery.data.total}
                onClick={() => setParam("page", page + 1)}
                className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
              >
                Next
              </button>
            </div>
          </>
        )}
      </Card>
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
        value
          ? "border-cyan/30 bg-cyan/10 text-cyan"
          : "border-border text-text-muted hover:bg-surface-hover",
      ].join(" ")}
    >
      {value ? "✓" : "○"} {label}
    </button>
  );
}
