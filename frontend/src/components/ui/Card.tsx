import { type HTMLAttributes, type ReactNode } from "react";

export interface CardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
}

/** Reusable surface used for stat panels, table containers, etc. */
export function Card({ children, className = "", ...rest }: CardProps) {
  return (
    <div
      {...rest}
      className={`rounded-lg border border-border bg-surface p-[22px] ${className}`}
    >
      {children}
    </div>
  );
}
