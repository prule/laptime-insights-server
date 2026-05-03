import { useSessionOptions, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, EmptyState, LoadingState } from "../components/ui/States";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import { SessionRow } from "../components/SessionRow";
import { getString, useUrlState } from "../hooks/useUrlState";

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

  const sessionsQuery = useSessions({ ...filters, size: 50, sort: "startedAt:DESC" });

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
          <div className="flex flex-col gap-1">
            {sessionsQuery.data.items.map((s) => (
              <SessionRow key={s.uid} session={s} />
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
