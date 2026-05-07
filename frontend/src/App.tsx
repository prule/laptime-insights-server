import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { CompareScreen } from "./screens/CompareScreen";
import { LiveScreen } from "./screens/LiveScreen";
import { OverviewScreen } from "./screens/OverviewScreen";
import { SessionsScreen } from "./screens/SessionsScreen";
import { SessionDetailScreen } from "./screens/SessionDetailScreen";
import { LapsScreen } from "./screens/LapsScreen";

export function App() {
  return (
    <AppShell>
      <Routes>
        <Route index element={<OverviewScreen />} />
        <Route path="sessions" element={<SessionsScreen />} />
        <Route path="sessions/:uid" element={<SessionDetailScreen />} />
        <Route path="laps" element={<LapsScreen />} />
        <Route path="compare" element={<CompareScreen />} />
        <Route path="live" element={<LiveScreen />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  );
}
