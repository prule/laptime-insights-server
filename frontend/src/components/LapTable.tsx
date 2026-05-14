import { useMemo } from "react";
import type { ReactNode } from "react";
import type { LapResource } from "../api/types";
import { useFeatureEnabled } from "../providers/FeaturesProvider";
import { formatDate, formatLapTime, formatTime } from "../lib/format";
import { SortableHeader, type SortState } from "./ui/SortableHeader";

export type { SortOrder, SortState } from "./ui/SortableHeader";

export interface LapTableColumn {
  header: ReactNode;
  width: string;
  cell: (lap: LapResource) => ReactNode;
}

export interface LapTableProps {
  laps: LapResource[];
  /**
   * Extra columns appended after the core nine. Used by calling pages to inject
   * context-specific actions (e.g. compare buttons) without breaking the shared
   * column shape.
   */
  extraColumns?: LapTableColumn[];
  /**
   * Optional column prepended before the core columns. Used for affordances like
   * a select-mode checkbox that sit visually to the left of the data.
   */
  prefixColumn?: {
    header?: ReactNode;
    width?: string;
    cell: (lap: LapResource) => ReactNode;
  };
  onRowClick?: (lap: LapResource) => void;
  /** When provided, the session UID cell becomes a clickable link. Omit in
   *  contexts where navigation away would be disruptive (e.g. picker modals). */
  onSessionClick?: (sessionUid: string) => void;
  isRowDisabled?: (lap: LapResource) => boolean;
  isRowSelected?: (lap: LapResource) => boolean;
  /** Visually muted — used for competitor laps on the session detail screen. */
  isRowDimmed?: (lap: LapResource) => boolean;
  disabledTitle?: string;
  /**
   * Field names the backend advertises as sortable for this collection (from `Page.sortable`).
   * Headers whose mapped field appears here become clickable. Omit (or pass `[]`) to disable
   * header-driven sorting entirely — keeps picker/embedded contexts unchanged.
   */
  sortableFields?: string[];
  /** Currently applied sort, or null when unsorted. Drives the arrow indicator on the header. */
  sort?: SortState | null;
  /**
   * Called when the user clicks a sortable header. Receives the next sort state, or `null` when
   * the user toggled the active column off. Caller is responsible for re-fetching with the new
   * sort and (typically) pushing it to URL state.
   */
  onSortChange?: (next: SortState | null) => void;
}

function formatDelta(lapMs: number, bestMs: number): string {
  const diff = lapMs - bestMs;
  if (diff === 0) return "±0.000";
  const sign = diff > 0 ? "+" : "−";
  return `${sign}${(Math.abs(diff) / 1000).toFixed(3)}`;
}

const CORE_WIDTHS = [
  "90px",  // session uid
  "40px",  // player indicator
  "1fr",   // track
  "150px", // date/time
  "50px",  // lap #
  "55px",  // car #
  "1fr",   // car name
  "110px", // lap time
  "80px",  // status
  "50px",  // pb
  "95px",  // to best
];

export function LapTable({
  laps,
  extraColumns = [],
  prefixColumn,
  onRowClick,
  onSessionClick,
  isRowDisabled,
  isRowSelected,
  isRowDimmed,
  disabledTitle,
  sortableFields,
  sort,
  onSortChange,
}: LapTableProps) {
  // The session column is clickable only when the Sessions UI is on. The lap's `session` rel
  // exists regardless — that's a capability link — but navigating there would be pointless if
  // the route is hidden.
  const sessionsEnabled = useFeatureEnabled("sessions");
  // Compute session best (minimum valid lapTime) per session from the provided
  // laps. For single-session views this equals the session best; for cross-
  // session views it reflects the best among whichever laps are in view.
  const sessionBestMap = useMemo(() => {
    const map = new Map<string, number>();
    for (const lap of laps) {
      if (!lap.valid) continue;
      const cur = map.get(lap.sessionUid);
      if (cur === undefined || lap.lapTime < cur) map.set(lap.sessionUid, lap.lapTime);
    }
    return map;
  }, [laps]);

  const allWidths = [
    ...(prefixColumn ? [prefixColumn.width ?? "36px"] : []),
    ...CORE_WIDTHS,
    ...extraColumns.map((c) => c.width),
  ];
  const gridStyle = { gridTemplateColumns: allWidths.join(" ") };

  // Header → backend sort-field. Omitted columns are never sortable (derived columns like "To
  // best", capability columns like Session). Headers whose field isn't in the backend-advertised
  // `sortableFields` list still render — `SortableHeader` falls back to a plain label.
  const renderHeader = (label: string, field: string | null) => {
    if (!field || !onSortChange) return <span>{label}</span>;
    return (
      <SortableHeader
        label={label}
        field={field}
        sort={sort ?? null}
        sortableFields={sortableFields}
        onChange={onSortChange}
      />
    );
  };

  return (
    <div className="overflow-hidden rounded border border-border">
      <div
        className="grid items-center gap-3 border-b border-border bg-surface-active px-3 py-2 font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted"
        style={gridStyle}
      >
        {prefixColumn && <div>{prefixColumn.header ?? ""}</div>}
        <div>Session</div>
        <div title="Player lap">P</div>
        {renderHeader("Track", "track")}
        {renderHeader("Date / Time", "recordedAt")}
        {renderHeader("Lap", "lapNumber")}
        {renderHeader("Car #", "carId")}
        <div>Car</div>
        {renderHeader("Lap time", "lapTime")}
        {renderHeader("Status", "valid")}
        <div>PB</div>
        <div>To best</div>
        {extraColumns.map((col, i) => (
          <div key={i}>{col.header}</div>
        ))}
      </div>

      {laps.map((lap) => {
        const disabled = isRowDisabled?.(lap) ?? false;
        const selected = isRowSelected?.(lap) ?? false;
        const dimmed = isRowDimmed?.(lap) ?? false;
        const bestMs = sessionBestMap.get(lap.sessionUid) ?? null;

        const lapTimeClass = `font-mono text-sm ${
          lap.personalBest && !dimmed ? "text-ok" : lap.valid ? "text-text" : "text-text-dim"
        }`;

        const deltaClass =
          lap.valid && bestMs !== null
            ? lap.lapTime === bestMs
              ? "text-ok"
              : "text-text-muted"
            : "text-text-dim";

        const cells = (
          <>
            {prefixColumn && <div>{prefixColumn.cell(lap)}</div>}
            <div className="truncate" title={lap.sessionUid}>
              {/* UI gate: render as a link only when the Sessions UI feature is enabled. The
                  `lap._links.session` rel is always present (it's a capability), so we read the
                  UI toggle instead. */}
              {onSessionClick && sessionsEnabled ? (
                <button
                  type="button"
                  onClick={(e) => { e.stopPropagation(); onSessionClick(lap.sessionUid); }}
                  className="font-mono text-[10px] text-cyan/70 hover:text-cyan hover:underline"
                >
                  {lap.sessionUid.slice(0, 8)}
                </button>
              ) : (
                <span className="font-mono text-[10px] text-text-dim">
                  {lap.sessionUid.slice(0, 8)}
                </span>
              )}
            </div>
            <div className="flex items-center justify-center" title={lap.playerLap === null ? "Unknown" : lap.playerLap ? "Player lap" : "Competitor lap"}>
              {lap.playerLap === true && (
                <span className="inline-block h-2 w-2 rounded-full bg-cyan" />
              )}
              {lap.playerLap === false && (
                <span className="inline-block h-2 w-2 rounded-full bg-text-dim" />
              )}
            </div>
            <div className="min-w-0 truncate font-sans text-[13px] text-text">
              {lap.track ?? <span className="text-text-dim">—</span>}
            </div>
            <div className="whitespace-nowrap font-mono text-xs text-text-muted">
              {formatDate(lap.recordedAt)}
              <span className="mx-1 text-text-dim">·</span>
              {formatTime(lap.recordedAt)}
            </div>
            <div className="font-mono text-xs text-text-muted">#{lap.lapNumber}</div>
            <div className="font-mono text-xs text-text-muted">{lap.carId}</div>
            <div className="min-w-0 truncate font-sans text-[12px] text-text-muted">
              {lap.car ?? <span className="text-text-dim">unknown</span>}
            </div>
            <div className={lapTimeClass}>{formatLapTime(lap.lapTime)}</div>
            <div className="font-mono text-[11px]">
              {lap.valid ? (
                <span className="text-text-muted">Valid</span>
              ) : (
                <span className="text-accent">INVALID</span>
              )}
            </div>
            <div className="font-mono text-[11px]">
              {lap.personalBest && !dimmed ? (
                <span className="text-ok">PB</span>
              ) : (
                <span className="text-text-dim">—</span>
              )}
            </div>
            <div className={`font-mono text-[11px] ${deltaClass}`}>
              {lap.valid && bestMs !== null ? formatDelta(lap.lapTime, bestMs) : "—"}
            </div>
            {extraColumns.map((col, i) => (
              <div key={i}>{col.cell(lap)}</div>
            ))}
          </>
        );

        const baseClass = [
          "grid items-center gap-3 border-b border-border/40 px-3 py-2 last:border-b-0",
          dimmed ? "opacity-75" : "",
        ].join(" ");

        if (onRowClick) {
          return (
            <button
              key={lap.uid}
              type="button"
              onClick={() => !disabled && onRowClick(lap)}
              disabled={disabled}
              title={disabled ? disabledTitle : undefined}
              className={[
                baseClass,
                "w-full text-left",
                disabled
                  ? "cursor-not-allowed opacity-30"
                  : selected
                    ? "bg-cyan/10 hover:bg-cyan/15"
                    : "hover:bg-surface-hover",
              ].join(" ")}
              style={gridStyle}
            >
              {cells}
            </button>
          );
        }

        return (
          <div
            key={lap.uid}
            className={[
              baseClass,
              selected ? "bg-cyan/10" : "hover:bg-surface-hover",
            ].join(" ")}
            style={gridStyle}
          >
            {cells}
          </div>
        );
      })}
    </div>
  );
}
