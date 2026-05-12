import { Navigate, Route, Routes } from "react-router-dom";
import { AppShell } from "./components/layout/AppShell";
import { FEATURE_CONFIG, FEATURES } from "./config/features";
import { useFeatures } from "./providers/FeaturesProvider";

export function App() {
  const { isEnabled } = useFeatures();
  const enabled = FEATURES.filter(isEnabled);
  // Pick a sensible fallback target for routes the user can't reach — first enabled feature's
  // nav path, or `/` if literally nothing is on (which only happens when the backend says so).
  const fallback = enabled[0] ? FEATURE_CONFIG[enabled[0]].nav.path : "/";

  return (
    <AppShell>
      <Routes>
        {enabled.flatMap((f) =>
          FEATURE_CONFIG[f].routes.map((r) => (
            <Route key={`${f}:${r.path}`} path={r.path} element={r.element} />
          )),
        )}
        <Route path="*" element={<Navigate to={fallback} replace />} />
      </Routes>
    </AppShell>
  );
}
