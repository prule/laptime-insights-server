import type { Config } from "tailwindcss";

const config: Config = {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        bg: "#080a0f",
        surface: "#0d1117",
        "surface-hover": "#111827",
        "surface-active": "#141c28",
        sidebar: "#0a0d14",
        border: "rgb(255 255 255 / 0.06)",
        "border-hover": "rgb(255 255 255 / 0.12)",
        text: "#e8eaf0",
        "text-muted": "#6b7280",
        "text-dim": "#3d4451",
        accent: "#e8212a",
        cyan: "#00d4ff",
        ok: "#22c55e",
        warn: "#eab308",
        orange: "#f97316",
      },
      fontFamily: {
        sans: ["DM Sans", "system-ui", "sans-serif"],
        mono: ["DM Mono", "Courier New", "monospace"],
      },
    },
  },
  plugins: [],
};

export default config;
