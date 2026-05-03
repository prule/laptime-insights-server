export interface NavItem {
  id: string;
  label: string;
  icon: string;
  path: string;
}

export const NAV_ITEMS: NavItem[] = [
  { id: "overview", label: "Overview", icon: "◈", path: "/" },
  { id: "sessions", label: "Sessions", icon: "◫", path: "/sessions" },
  { id: "laps", label: "Laps", icon: "◷", path: "/laps" },
];
