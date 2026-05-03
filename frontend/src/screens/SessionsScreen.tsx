import { useState } from "react";
import { useSessionOptions, useSessions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, EmptyState, LoadingState } from "../components/ui/States";
import { SectionHeader } from "../components/ui/SectionHeader";
import { SessionRow } from "../components/SessionRow";

interface Filters {
  car?: string;
  track?: string;
  simulator?: string;
}

export function SessionsScreen() {
  const optionsQuery = useSessionOptions();
  const [filters, setFilters] = useState<Filters>({});
  const sessionsQuery = useSessions({ ...filters, size: 50, sort: "startedAt:DESC" });

  const setFilter = (key: keyof Filters, value: string | undefined) => {
    setFilters((prev) => ({ ...prev, [key]: value === "" ? undefined : value }));
  };

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader title="Filter" sub="Server-side filters via /api/1/sessions query params" />
        <div className="flex flex-wrap gap-3">
          <FilterSelect
            label="Track"
            value={filters.track}
            options={optionsQuery.data?.tracks ?? []}
            onChange={(v) => setFilter("track", v)}
          />
          <FilterSelect
            label="Car"
            value={filters.car}
            options={optionsQuery.data?.cars ?? []}
            onChange={(v) => setFilter("car", v)}
          />
          <FilterSelect
            label="Simulator"
            value={filters.simulator}
            options={optionsQuery.data?.simulators ?? []}
            onChange={(v) => setFilter("simulator", v)}
          />
          {(filters.track || filters.car || filters.simulator) && (
            <button
              onClick={() => setFilters({})}
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

function FilterSelect({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: string | undefined;
  options: string[];
  onChange: (value: string | undefined) => void;
}) {
  return (
    <label className="flex flex-col gap-1">
      <span className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">{label}</span>
      <select
        value={value ?? ""}
        onChange={(e) => onChange(e.target.value || undefined)}
        className="rounded border border-border bg-surface px-3 py-2 font-sans text-[13px] text-text outline-none focus:border-cyan/40"
      >
        <option value="">All</option>
        {options.map((opt) => (
          <option key={opt} value={opt}>
            {opt}
          </option>
        ))}
      </select>
    </label>
  );
}
