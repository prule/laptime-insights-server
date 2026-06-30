import { NavLink } from "react-router-dom";
import { useTogglePublicProfile } from "../../api/profile";
import { FEATURE_CONFIG, FEATURES } from "../../config/features";
import { useDataMode } from "../../providers/DataModeProvider";
import { useFeatures } from "../../providers/FeaturesProvider";

export function Sidebar() {
  const { mode, apiUrl, setMode } = useDataMode();
  const { isEnabled } = useFeatures();
  const isLive = mode === "live";
  const navItems = FEATURES.filter(isEnabled).map((f) => ({ id: f, ...FEATURE_CONFIG[f].nav }));
  const profileEnabled = isEnabled("public-profile");
  const toggleProfile = useTogglePublicProfile();

  return (
    <aside className="flex h-full w-[220px] flex-shrink-0 flex-col border-r border-border bg-sidebar">
      <div className="flex items-center gap-[10px] border-b border-border px-5 py-[22px]">
        <div className="flex h-[30px] w-[30px] items-center justify-center rounded-[7px] bg-gradient-to-br from-accent to-orange font-mono text-[13px] font-bold text-white">
          L
        </div>
        <div>
          <div className="font-mono text-[13px] font-medium tracking-tight text-text">LapTime</div>
          <div className="font-mono text-[10px] tracking-[0.05em] text-text-muted">INSIGHTS</div>
        </div>
      </div>

      <nav className="flex-1 py-3">
        {navItems.map((item) => (
          <NavLink
            key={item.id}
            to={item.path}
            end={item.end ?? false}
            className={({ isActive }) =>
              [
                "mb-[2px] flex items-center gap-[10px] rounded-r-md border-l-2 px-[18px] py-[10px] text-left transition-colors",
                isActive
                  ? "border-accent bg-white/[0.06] text-text"
                  : "border-transparent text-text-muted hover:bg-white/[0.03]",
              ].join(" ")
            }
          >
            {({ isActive }) => (
              <>
                <span className={`text-base ${isActive ? "text-accent" : "text-text-muted"}`}>{item.icon}</span>
                <span className="font-sans text-[13px] font-medium">{item.label}</span>
              </>
            )}
          </NavLink>
        ))}
      </nav>

      {isLive && (
        <div className="border-t border-border px-4 pt-4">
          <button
            onClick={() => toggleProfile.mutate(!profileEnabled)}
            disabled={toggleProfile.isPending}
            className={[
              "flex w-full items-center justify-between rounded-md border px-3 py-[9px] text-left transition-colors disabled:opacity-50",
              profileEnabled
                ? "border-cyan/25 bg-cyan/10"
                : "border-border bg-white/[0.04] hover:bg-white/[0.06]",
            ].join(" ")}
            title={profileEnabled ? "Turn the public profile off" : "Turn the public profile on"}
          >
            <span className="font-mono text-[10px] tracking-[0.08em] text-text-muted">
              PUBLIC PROFILE
            </span>
            <span
              className={`font-mono text-[10px] tracking-[0.08em] ${profileEnabled ? "text-cyan" : "text-text-muted"}`}
            >
              {profileEnabled ? "ON" : "OFF"}
            </span>
          </button>
        </div>
      )}

      <div className="border-t border-border p-4">
        <button
          onClick={() => setMode(isLive ? "mock" : "live")}
          className={[
            "flex w-full items-center gap-2 rounded-md border px-3 py-[9px] text-left transition-colors",
            isLive
              ? "border-ok/25 bg-ok/10"
              : "border-border bg-white/[0.04] hover:bg-white/[0.06]",
          ].join(" ")}
          title={isLive ? "Switch to mock data" : "Switch to live data"}
        >
          <span
            className={[
              "h-[7px] w-[7px] flex-shrink-0 rounded-full",
              isLive ? "bg-ok shadow-[0_0_6px_#22c55e]" : "bg-text-muted",
            ].join(" ")}
          />
          <div className="text-left">
            <div
              className={`font-mono text-[10px] tracking-[0.08em] ${isLive ? "text-ok" : "text-text-muted"}`}
            >
              {isLive ? "LIVE" : "MOCK"}
            </div>
            {isLive && (
              <div className="truncate font-mono text-[9px] text-text-muted/70">
                {apiUrl || "(relative URL via dev proxy)"}
              </div>
            )}
          </div>
        </button>
      </div>
    </aside>
  );
}
