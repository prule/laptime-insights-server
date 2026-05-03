import { jsx as _jsx } from "react/jsx-runtime";
import React from "react";
import ReactDOM from "react-dom/client";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BrowserRouter } from "react-router-dom";
import { App } from "./App";
import { DataModeProvider } from "./providers/DataModeProvider";
import { TimeRangeProvider } from "./providers/TimeRangeProvider";
import "./styles.css";
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            staleTime: 30_000,
            refetchOnWindowFocus: false,
            retry: 1,
        },
    },
});
ReactDOM.createRoot(document.getElementById("root")).render(_jsx(React.StrictMode, { children: _jsx(QueryClientProvider, { client: queryClient, children: _jsx(DataModeProvider, { children: _jsx(TimeRangeProvider, { children: _jsx(BrowserRouter, { children: _jsx(App, {}) }) }) }) }) }));
