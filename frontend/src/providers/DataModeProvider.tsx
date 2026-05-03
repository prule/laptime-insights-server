import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";

export type DataMode = "mock" | "live";

interface DataModeContextValue {
  mode: DataMode;
  apiUrl: string;
  setMode: (mode: DataMode) => void;
  setApiUrl: (url: string) => void;
}

const STORAGE_KEY = "lti.dataMode";
const URL_KEY = "lti.apiUrl";

const DEFAULTS = {
  mode: "mock" as DataMode,
  // Empty string makes fetch use a relative URL (matches the Vite dev proxy).
  apiUrl: "",
};

const DataModeContext = createContext<DataModeContextValue | null>(null);

function readMode(): DataMode {
  if (typeof window === "undefined") return DEFAULTS.mode;
  const stored = window.localStorage.getItem(STORAGE_KEY);
  return stored === "live" || stored === "mock" ? stored : DEFAULTS.mode;
}

function readUrl(): string {
  if (typeof window === "undefined") return DEFAULTS.apiUrl;
  return window.localStorage.getItem(URL_KEY) ?? DEFAULTS.apiUrl;
}

export function DataModeProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<DataMode>(readMode);
  const [apiUrl, setApiUrlState] = useState<string>(readUrl);

  const setMode = useCallback((next: DataMode) => {
    setModeState(next);
    window.localStorage.setItem(STORAGE_KEY, next);
  }, []);

  const setApiUrl = useCallback((next: string) => {
    setApiUrlState(next);
    window.localStorage.setItem(URL_KEY, next);
  }, []);

  const value = useMemo(
    () => ({ mode, apiUrl, setMode, setApiUrl }),
    [mode, apiUrl, setMode, setApiUrl],
  );

  return <DataModeContext.Provider value={value}>{children}</DataModeContext.Provider>;
}

export function useDataMode(): DataModeContextValue {
  const ctx = useContext(DataModeContext);
  if (!ctx) throw new Error("useDataMode must be used inside DataModeProvider");
  return ctx;
}
