/**
 * Labelled `<select>` for single-value filters. Pass `value === undefined` to
 * mean "no filter applied"; the empty `<option>` round-trips through that.
 */
export interface FilterSelectProps {
  label: string;
  value: string | undefined;
  options: string[];
  onChange: (value: string | undefined) => void;
}

export function FilterSelect({ label, value, options, onChange }: FilterSelectProps) {
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
