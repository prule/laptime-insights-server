import { type ReactNode } from "react";

export function LoadingState({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="flex h-32 items-center justify-center font-mono text-xs uppercase tracking-[0.08em] text-text-muted">
      {label}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof Error ? error.message : String(error);
  return (
    <div className="flex h-32 flex-col items-center justify-center gap-2 font-sans text-sm text-accent">
      <div>Failed to load data</div>
      <div className="font-mono text-[11px] text-text-muted">{message}</div>
      {onRetry && (
        <button
          onClick={onRetry}
          className="rounded border border-border px-3 py-1 font-mono text-[11px] uppercase tracking-[0.08em] text-text hover:bg-surface-hover"
        >
          Retry
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, description }: { title: string; description?: ReactNode }) {
  return (
    <div className="flex h-32 flex-col items-center justify-center gap-1 text-center font-sans text-sm text-text-muted">
      <div className="text-text">{title}</div>
      {description && <div className="text-xs">{description}</div>}
    </div>
  );
}
