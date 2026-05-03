import { type ReactNode } from "react";

export interface SectionHeaderProps {
  title: string;
  sub?: ReactNode;
  action?: string;
  onAction?: () => void;
}

export function SectionHeader({ title, sub, action, onAction }: SectionHeaderProps) {
  return (
    <div className="mb-4 flex items-baseline justify-between">
      <div>
        <div className="font-sans text-sm font-medium text-text">{title}</div>
        {sub && <div className="font-sans text-xs text-text-muted">{sub}</div>}
      </div>
      {action && (
        <button
          onClick={onAction}
          className="font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted transition-colors hover:text-cyan"
        >
          {action}
        </button>
      )}
    </div>
  );
}
