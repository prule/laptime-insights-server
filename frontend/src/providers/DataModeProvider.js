import { jsx as _jsx } from "react/jsx-runtime";
import { createContext, useCallback, useContext, useMemo, useState } from "react";
const STORAGE_KEY = "lti.dataMode";
const URL_KEY = "lti.apiUrl";
const DEFAULTS = {
    mode: "mock",
    // Empty string makes fetch use a relative URL (matches the Vite dev proxy).
    apiUrl: "",
};
const DataModeContext = createContext(null);
function readMode() {
    if (typeof window === "undefined")
        return DEFAULTS.mode;
    const stored = window.localStorage.getItem(STORAGE_KEY);
    return stored === "live" || stored === "mock" ? stored : DEFAULTS.mode;
}
function readUrl() {
    if (typeof window === "undefined")
        return DEFAULTS.apiUrl;
    return window.localStorage.getItem(URL_KEY) ?? DEFAULTS.apiUrl;
}
export function DataModeProvider({ children }) {
    const [mode, setModeState] = useState(readMode);
    const [apiUrl, setApiUrlState] = useState(readUrl);
    const setMode = useCallback((next) => {
        setModeState(next);
        window.localStorage.setItem(STORAGE_KEY, next);
    }, []);
    const setApiUrl = useCallback((next) => {
        setApiUrlState(next);
        window.localStorage.setItem(URL_KEY, next);
    }, []);
    const value = useMemo(() => ({ mode, apiUrl, setMode, setApiUrl }), [mode, apiUrl, setMode, setApiUrl]);
    return _jsx(DataModeContext.Provider, { value: value, children: children });
}
export function useDataMode() {
    const ctx = useContext(DataModeContext);
    if (!ctx)
        throw new Error("useDataMode must be used inside DataModeProvider");
    return ctx;
}
