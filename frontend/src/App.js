import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { CompareScreen } from "./screens/CompareScreen";
import { OverviewScreen } from "./screens/OverviewScreen";
import { SessionsScreen } from "./screens/SessionsScreen";
import { SessionDetailScreen } from "./screens/SessionDetailScreen";
import { LapsScreen } from "./screens/LapsScreen";
export function App() {
    return (_jsx(AppShell, { children: _jsxs(Routes, { children: [_jsx(Route, { index: true, element: _jsx(OverviewScreen, {}) }), _jsx(Route, { path: "sessions", element: _jsx(SessionsScreen, {}) }), _jsx(Route, { path: "sessions/:uid", element: _jsx(SessionDetailScreen, {}) }), _jsx(Route, { path: "laps", element: _jsx(LapsScreen, {}) }), _jsx(Route, { path: "compare", element: _jsx(CompareScreen, {}) }), _jsx(Route, { path: "*", element: _jsx(Navigate, { to: "/", replace: true }) })] }) }));
}
