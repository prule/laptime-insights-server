import { useSessionOptions, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, EmptyState, LoadingState } from "../components/ui/States";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import {
  formatSortParam,
  parseSortParam,
  SortableHeader,
  type SortState,
} from "../components/ui/SortableHeader";
import { SessionRow } from "../components/SessionRow";
import { getString, useUrlState } from "../hooks/useUrlState";
import { useTimeRange } from "../providers/TimeRangeProvider";

/** Mirrors the grid template used inside `SessionRow` so the header aligns with each row. */
const SESSIONS_GRID_TEMPLATE = "90px 1fr 120px 90px 140px";

/**
 * Filters live in the URL querystring. Reload-safe and shareable: a link
 * like `/sessions?track=Monza&car=Ferrari%20488%20GT3` restores the same
 * filter state.
 */
export function SessionsScreen() {
  const optionsQuery = useSessionOptions();
  const [params, setParam, setMany] = useUrlState();

  const filters = {
    track: getString(params, "track"),
    car: getString(params, "car"),
    simulator: getString(params, "simulator"),
  };
  const facetsActive = !!(filters.track || filters.car || filters.simulator);

  const { fromIso } = useTimeRange();

  // Sort lives in the URL (`sort=field:ORDER`) so deep-links restore ordering. Default keeps the
  // historical "most recent first" framing — `startedAt:DESC`.
  const sort: SortState = parseSortParam(getString(params, "sort")) ?? {
    field: "startedAt",
    order: "DESC",
  };
  const setSort = (next: SortState | null) => {
    setMany({ sort: formatSortParam(next), page: undefined });
  };

  const sessionsQuery = useSessions({
    ...filters,
    from: fromIso ?? undefined,
    size: 50,
    sort: formatSortParam(sort),
  });

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader title="Filter" sub="Filters are written to the URL — reload or share the link to restore state" />
        <div className="flex flex-wrap gap-3">
          <FilterSelect
            label="Track"
            value={filters.track}
            options={optionsQuery.data?.tracks ?? []}
            onChange={(v) => setParam("track", v)}
          />
          <FilterSelect
            label="Car"
            value={filters.car}
            options={optionsQuery.data?.cars ?? []}
            onChange={(v) => setParam("car", v)}
          />
          <FilterSelect
            label="Simulator"
            value={filters.simulator}
            options={optionsQuery.data?.simulators ?? []}
            onChange={(v) => setParam("simulator", v)}
          />
          {facetsActive && (
            <button
              onClick={() => setMany({ track: undefined, car: undefined, simulator: undefined })}
              className="self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
            >
              Clear
            </button>
          )}
        </div>
      </Card>

      <Card>
        <SectionHeader
          title="Sessions"
          sub={
            sessionsQuery.data
              ? `${sessionsQuery.data.total} match · showing ${sessionsQuery.data.items.length}`
              : undefined
          }
        />
        {sessionsQuery.isLoading && <LoadingState />}
        {sessionsQuery.isError && (
          <ErrorState error={sessionsQuery.error} onRetry={() => sessionsQuery.refetch()} />
        )}
        {sessionsQuery.data && sessionsQuery.data.items.length === 0 && (
          <EmptyState title="No sessions match" description="Try clearing filters or seeding the database." />
        )}
        {sessionsQuery.data && sessionsQuery.data.items.length > 0 && (
          <div className="flex flex-col">
            <div
              className="grid items-center gap-3 border-b border-border bg-surface-active px-[10px] py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted"
              style={{ gridTemplateColumns: SESSIONS_GRID_TEMPLATE }}
            >
              <SortableHeader
                label="Date / Time"
                field="startedAt"
                sort={sort}
                sortableFields={sessionsQuery.data.sortable}
                onChange={setSort}
              />
              <div className="flex items-center gap-2">
                <SortableHeader
                  label="Track"
                  field="track"
                  sort={sort}
                  sortableFields={sessionsQuery.data.sortable}
                  onChange={setSort}
                />
                <span className="text-text-dim">·</span>
                <SortableHeader
                  label="Car"
                  field="car"
                  sort={sort}
                  sortableFields={sessionsQuery.data.sortable}
                  onChange={setSort}
                />
              </div>
              <SortableHeader
                label="Type"
                field="sessionType"
                sort={sort}
                sortableFields={sessionsQuery.data.sortable}
                onChange={setSort}
              />
              <SortableHeader
                label="Sim"
                field="simulator"
                sort={sort}
                sortableFields={sessionsQuery.data.sortable}
                onChange={setSort}
              />
              <SortableHeader
                label="Driving"
                field="drivingTimeMs"
                sort={sort}
                sortableFields={sessionsQuery.data.sortable}
                onChange={setSort}
              />
            </div>
            <div className="flex flex-col gap-1 pt-1">
              {sessionsQuery.data.items.map((s) => (
                <SessionRow key={s.uid} session={s} />
              ))}
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}
