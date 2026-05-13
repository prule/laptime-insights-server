import type { UseQueryResult } from "@tanstack/react-query";

/**
 * Collapse a list of TanStack queries into a single loading / first-error view. The combined
 * `isLoading` is true as long as **any** query is still loading — useful for screens that should
 * render a single page-level spinner instead of a half-populated UI while late-arriving requests
 * fill in. `error` is the first non-null error in the supplied order, so callers control which
 * failure surfaces first.
 */
export function combineQueryState(
  queries: Pick<UseQueryResult<unknown>, "isLoading" | "isError" | "error">[],
): { isLoading: boolean; error: unknown } {
  return {
    isLoading: queries.some((q) => q.isLoading),
    error: queries.find((q) => q.isError)?.error ?? null,
  };
}
