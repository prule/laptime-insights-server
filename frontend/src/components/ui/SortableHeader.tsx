import type { ReactNode } from "react";

export type SortOrder = "ASC" | "DESC";
export interface SortState {
  field: string;
  order: SortOrder;
}

export interface SortableHeaderProps {
  /** Display label for the column. */
  label: ReactNode;
  /** Backend sort-field name this header drives. */
  field: string;
  /** Currently applied sort across the table, or null when unsorted. */
  sort: SortState | null;
  /**
   * Field names the backend advertises as sortable. When `field` is absent from this list the
   * header renders as a plain (non-clickable) label — the column is shown but can't be sorted.
   */
  sortableFields: string[] | undefined;
  /** Called with the next sort state (or null to clear) when the header is clicked. */
  onChange: (next: SortState | null) => void;
}

/**
 * A column header that cycles its column's sort state on click: unsorted → ASC → DESC → unsorted.
 * Visually indicates the active column with a coloured arrow; inactive columns show a faint ↕.
 *
 * Used by both `LapsScreen` and `SessionsScreen` so the sort affordance stays consistent across
 * tables driven by `Page.sortable`.
 */
export function SortableHeader({
  label,
  field,
  sort,
  sortableFields,
  onChange,
}: SortableHeaderProps) {
  const enabled = (sortableFields ?? []).includes(field);
  if (!enabled) return <span>{label}</span>;

  const active = sort?.field === field;
  const next: SortState | null = !active
    ? { field, order: "ASC" }
    : sort!.order === "ASC"
      ? { field, order: "DESC" }
      : null;

  const arrow = active ? (sort!.order === "ASC" ? "▲" : "▼") : "↕";
  const title = !active
    ? `Sort by ${typeof label === "string" ? label : field} ascending`
    : sort!.order === "ASC"
      ? `Sort descending`
      : `Clear sort`;

  return (
    <button
      type="button"
      onClick={() => onChange(next)}
      title={title}
      className={[
        "flex items-center gap-1 text-left uppercase tracking-[0.08em]",
        active ? "text-cyan" : "hover:text-text",
      ].join(" ")}
    >
      <span>{label}</span>
      <span aria-hidden className="text-[8px] opacity-70">
        {arrow}
      </span>
    </button>
  );
}

/** Parse a `field:ORDER` querystring value into a SortState, or null when invalid/absent. */
export function parseSortParam(raw: string | undefined): SortState | null {
  if (!raw) return null;
  const [field, order] = raw.split(":");
  if (!field || (order !== "ASC" && order !== "DESC")) return null;
  return { field, order };
}

/** Serialise a SortState back to the `field:ORDER` querystring shape (or undefined when null). */
export function formatSortParam(sort: SortState | null): string | undefined {
  return sort ? `${sort.field}:${sort.order}` : undefined;
}
