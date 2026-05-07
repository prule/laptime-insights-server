export interface CarOption {
  value: string;
  label: string;
  /** When true, renders a cyan dot; false renders a dim dot; undefined renders no dot. */
  isPlayer?: boolean;
}

export interface CarFilterBarProps {
  cars: CarOption[];
  selected: string | null;
  onChange: (value: string | null) => void;
}

/** Pill-style car filter row. Renders null when there is only one car (nothing to filter). */
export function CarFilterBar({ cars, selected, onChange }: CarFilterBarProps) {
  if (cars.length <= 1) return null;
  return (
    <div className="flex flex-wrap items-center gap-1.5">
      <Pill label="All" active={selected === null} onClick={() => onChange(null)} />
      {cars.map((car) => (
        <Pill
          key={car.value}
          label={car.label}
          active={selected === car.value}
          isPlayer={car.isPlayer}
          onClick={() => onChange(selected === car.value ? null : car.value)}
        />
      ))}
    </div>
  );
}

function Pill({
  label,
  active,
  isPlayer,
  onClick,
}: {
  label: string;
  active: boolean;
  isPlayer?: boolean;
  onClick: () => void;
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      className={`flex items-center gap-1.5 rounded border px-2.5 py-1 font-mono text-[10px] uppercase tracking-[0.08em] transition-colors ${
        active
          ? "border-cyan/50 bg-cyan/10 text-cyan"
          : "border-border text-text-muted hover:border-cyan/30 hover:text-text"
      }`}
    >
      {isPlayer !== undefined && (
        <span
          className={`inline-block h-1.5 w-1.5 flex-shrink-0 rounded-full ${
            isPlayer ? "bg-cyan" : "bg-text-dim"
          }`}
        />
      )}
      {label}
    </button>
  );
}
