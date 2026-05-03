import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useLaps } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { SectionHeader } from "../components/ui/SectionHeader";
import { formatLapTime, formatTime } from "../lib/format";

const SIZE = 50;

export function LapsScreen() {
  const navigate = useNavigate();
  const [page, setPage] = useState(1);
  const [validOnly, setValidOnly] = useState(true);
  const [pbOnly, setPbOnly] = useState(false);

  const lapsQuery = useLaps({
    page,
    size: SIZE,
    sort: "lapTime:ASC",
    validLap: validOnly ? true : undefined,
    personalBest: pbOnly ? true : undefined,
  });

  const items = lapsQuery.data?.items ?? [];

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader title="Filter" />
        <div className="flex gap-3">
          <Toggle label="Valid only" value={validOnly} onChange={setValidOnly} />
          <Toggle label="Personal bests only" value={pbOnly} onChange={setPbOnly} />
        </div>
      </Card>

      <Card>
        <SectionHeader
          title="Fastest laps"
          sub={lapsQuery.data ? `${lapsQuery.data.total} total · page ${page}` : undefined}
        />
        {lapsQuery.isLoading && <LoadingState />}
        {lapsQuery.isError && (
          <ErrorState error={lapsQuery.error} onRetry={() => lapsQuery.refetch()} />
        )}
        {lapsQuery.data && items.length === 0 && (
          <EmptyState title="No laps match" description="Loosen the filters or seed the database." />
        )}
        {items.length > 0 && (
          <>
            <div className="overflow-hidden rounded border border-border">
              <div className="grid grid-cols-[60px_140px_120px_1fr_80px] items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
                <div>#</div>
                <div>Lap time</div>
                <div>Recorded</div>
                <div>Session</div>
                <div>Status</div>
              </div>
              {items.map((lap, i) => {
                const sessionUid = lap.sessionUid;
                return (
                  <button
                    key={lap.uid}
                    onClick={() => navigate(`/sessions/${sessionUid}`)}
                    className="grid w-full grid-cols-[60px_140px_120px_1fr_80px] items-center gap-3 border-b border-border/40 px-3 py-2 text-left last:border-b-0 hover:bg-surface-hover"
                  >
                    <div className="font-mono text-xs text-text-muted">{(page - 1) * SIZE + i + 1}</div>
                    <div className={`font-mono text-sm ${lap.personalBest ? "text-ok" : "text-text"}`}>
                      {formatLapTime(lap.lapTime)}
                    </div>
                    <div className="font-mono text-xs text-text-muted">{formatTime(lap.recordedAt)}</div>
                    <div className="font-mono text-xs text-text-muted truncate">{sessionUid}</div>
                    <div className="font-mono text-[11px]">
                      {lap.personalBest && <span className="text-ok">PB</span>}
                      {!lap.valid && <span className="text-accent">INVAL</span>}
                    </div>
                  </button>
                );
              })}
            </div>
            <div className="mt-3 flex items-center gap-2">
              <button
                disabled={page === 1}
                onClick={() => setPage((p) => Math.max(1, p - 1))}
                className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted disabled:opacity-30"
              >
                Prev
              </button>
              <button
                disabled={!lapsQuery.data || page * SIZE >= lapsQuery.data.total}
                onClick={() => setPage((p) => p + 1)}
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
        "rounded border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] transition-colors",
        value
          ? "border-cyan/30 bg-cyan/10 text-cyan"
          : "border-border text-text-muted hover:bg-surface-hover",
      ].join(" ")}
    >
      {value ? "✓" : "○"} {label}
    </button>
  );
}
