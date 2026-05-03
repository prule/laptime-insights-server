import { useEffect, type ReactNode } from "react";

export interface ModalProps {
  open: boolean;
  onClose: () => void;
  title?: string;
  children: ReactNode;
  /** Tailwind max-width class. Defaults to a comfortable browse width. */
  maxWidthClass?: string;
}

/**
 * Lightweight modal overlay. Backdrop click + Escape close it. No focus
 * trapping — we keep this simple and only use modals for short-lived,
 * pick-and-go interactions.
 */
export function Modal({ open, onClose, title, children, maxWidthClass = "max-w-3xl" }: ModalProps) {
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", onKey);
    document.body.style.overflow = "hidden";
    return () => {
      document.removeEventListener("keydown", onKey);
      document.body.style.overflow = "";
    };
  }, [open, onClose]);

  if (!open) return null;
  return (
    <div
      role="dialog"
      aria-modal
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-6"
      onClick={onClose}
    >
      <div
        className={`flex max-h-[85vh] w-full flex-col overflow-hidden rounded-lg border border-border bg-surface shadow-2xl ${maxWidthClass}`}
        onClick={(e) => e.stopPropagation()}
      >
        {title && (
          <div className="flex items-center justify-between border-b border-border px-5 py-3">
            <div className="font-sans text-sm font-medium text-text">{title}</div>
            <button
              onClick={onClose}
              aria-label="Close"
              className="font-mono text-[16px] leading-none text-text-muted hover:text-text"
            >
              ×
            </button>
          </div>
        )}
        <div className="flex-1 overflow-auto p-5">{children}</div>
      </div>
    </div>
  );
}
