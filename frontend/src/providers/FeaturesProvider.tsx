/**
 * Bootstraps the app by fetching `GET /api/1` and exposing which features the backend currently
 * advertises via HATEOAS `_links`. Every gating decision in the UI — sidebar, router, query
 * hooks, cross-screen action buttons — funnels through `useFeatureEnabled` so the toggle logic
 * lives in exactly one place.
 *
 * The fetch follows the active `DataMode`. In mock mode the in-memory handler answers with every
 * feature on; in live mode the Ktor backend's link map drives what's enabled. While the index is
 * loading we optimistically assume every feature is on so nav doesn't flicker — the first paint
 * matches the steady-state for the common "everything enabled" case, and disabled features simply
 * disappear once the response lands.
 */
import { createContext, useContext, useMemo, type ReactNode } from "react";
import { useQuery } from "@tanstack/react-query";
import { apiGet } from "../api/client";
import type { Links } from "../api/types";
import { FEATURES, type Feature } from "../config/features";
import { useDataMode } from "./DataModeProvider";

interface IndexResource {
  _links: Links;
}

interface FeaturesContextValue {
  links: Links;
  isEnabled: (feature: Feature) => boolean;
  isLoading: boolean;
  error: Error | null;
}

const ALL_LINKS: Links = Object.fromEntries(FEATURES.map((f) => [f, ""]));

const FeaturesContext = createContext<FeaturesContextValue | null>(null);

export function FeaturesProvider({ children }: { children: ReactNode }) {
  const { mode, apiUrl } = useDataMode();
  const query = useQuery({
    queryKey: ["index", mode, apiUrl] as const,
    queryFn: () => apiGet<IndexResource>({ mode, apiBase: apiUrl }, "/api/1"),
    staleTime: 5 * 60_000,
  });

  const value = useMemo<FeaturesContextValue>(() => {
    const links = query.data?._links ?? ALL_LINKS;
    return {
      links,
      isEnabled: (feature) => feature in links,
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
