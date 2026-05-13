/**
 * Bootstraps the app by fetching `GET /api/1`.
 *
 * Two pieces of information come back:
 * - `_links` — every API capability (always present). Hooks in `api/queries.ts` follow these
 *   for actual data fetching, so a screen can consume data from another feature's API even
 *   when that feature's UI is hidden.
 * - `enabledFeatures` — the UI surfaces the operator turned on. `useFeatureEnabled(feature)`
 *   reads from this list; the sidebar, router, and cross-screen action buttons gate on it.
 *
 * While the index is loading we optimistically assume every UI feature is on so nav doesn't
 * flicker — once the response lands, anything the backend hasn't advertised disappears.
 */
import { createContext, useContext, useMemo, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiGet } from "../api/client";
import type { Links } from "../api/types";
import { FEATURES, type Feature } from "../config/feature-types";
import { useDataMode } from "./DataModeProvider";

interface IndexResource {
  _links: Links;
  enabledFeatures: string[];
}

interface FeaturesContextValue {
  links: Links;
  isEnabled: (feature: Feature) => boolean;
  isLoading: boolean;
  error: Error | null;
}

const ALL_FEATURES: ReadonlySet<string> = new Set(FEATURES);

const FeaturesContext = createContext<FeaturesContextValue | null>(null);

export function FeaturesProvider({ children }: { children: ReactNode }) {
  const { mode, apiUrl } = useDataMode();
  const query = useQuery({
    queryKey: ["index", mode, apiUrl] as const,
    queryFn: () => apiGet<IndexResource>({ mode, apiBase: apiUrl }, "/api/1"),
    staleTime: 5 * 60_000,
  });

  const value = useMemo<FeaturesContextValue>(() => {
    const links = query.data?._links ?? {};
    const enabled = query.data?.enabledFeatures
      ? new Set<string>(query.data.enabledFeatures)
      : ALL_FEATURES;
    return {
      links,
      isEnabled: (feature) => enabled.has(feature),
      isLoading: query.isLoading,
      error: query.error as Error | null,
    };
  }, [query.data, query.isLoading, query.error]);

  return <FeaturesContext.Provider value={value}>{children}</FeaturesContext.Provider>;
}

export function useFeatures(): FeaturesContextValue {
  const ctx = useContext(FeaturesContext);
  if (!ctx) throw new Error("useFeatures must be used inside FeaturesProvider");
  return ctx;
}

export function useFeatureEnabled(feature: Feature): boolean {
  return useFeatures().isEnabled(feature);
}
