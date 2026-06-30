import type { RecordRow } from "../../api/profile";

const GRID = "grid grid-cols-[1.4fr_1.4fr_1fr_1fr_70px] items-center gap-3 px-5";

/** Records ledger: per circuit, the car, season best, all-time best + date, and a PB marker. */
export function RecordsLedger({ records }: { records: RecordRow[] }) {
  return (
    <div className="overflow-hidden rounded-lg border border-border bg-surface">
      <div
        className={`${GRID} border-b border-border py-[14px] font-mono text-[10px] tracking-[0.1em] text-text-dim`}
      >
        <div>CIRCUIT</div>
        <div>CAR</div>
        <div>SEASON BEST</div>
        <div>ALL-TIME BEST</div>
        <div className="text-right">PB</div>
      </div>
      {records.map((r) => (
        <div
          key={`${r.track}-${r.car}`}
          className={`${GRID} border-b border-border py-[14px] last:border-b-0`}
        >
          <div className="text-[13px] font-medium text-text">{r.track}</div>
          <div className="text-xs text-text-muted">{r.car}</div>
          <div className="font-mono text-sm text-text">{r.season}</div>
          <div className="font-mono text-sm text-ok">
            {r.allTime}
            <small className="block text-[9px] tracking-[0.05em] text-text-dim">{r.allTimeWhen}</small>
          </div>
          <div
            className={[
              "flex h-11 w-11 items-center justify-center justify-self-end rounded-full font-mono text-[9px] font-bold tracking-[0.05em]",
              r.isPB
                ? "border-[1.5px] border-warn/40 bg-[radial-gradient(circle,rgba(234,179,8,0.15),transparent_70%)] text-warn"
                : "border border-border text-text-dim",
            ].join(" ")}
          >
            {r.isPB ? "PB" : "—"}
          </div>
        </div>
      ))}
    </div>
  );
}
