import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { Sidebar } from "./Sidebar";
import { Topbar } from "./Topbar";
export function AppShell({ children }) {
    return (_jsxs("div", { className: "flex h-full w-full overflow-hidden", children: [_jsx(Sidebar, {}), _jsxs("div", { className: "flex flex-1 flex-col overflow-hidden", children: [_jsx(Topbar, {}), _jsx("main", { className: "flex-1 overflow-hidden", children: children })] })] }));
}
