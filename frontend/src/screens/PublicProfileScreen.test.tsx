// @vitest-environment jsdom
import { afterEach, describe, expect, it } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { cleanup, render, screen, within } from "@testing-library/react";
import { type ReactNode } from "react";
import { PublicProfileScreen } from "./PublicProfileScreen";
import { SAMPLE_PROFILE } from "../api/profile";
import { DataModeProvider } from "../providers/DataModeProvider";
import { FeaturesProvider } from "../providers/FeaturesProvider";

afterEach(cleanup);

// Wrap with the real providers in MOCK mode so the screen fetches SAMPLE_PROFILE through the mock
// handler exactly as it would fetch the backend snapshot in LIVE — no static data shortcut.
function renderScreen() {
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={queryClient}>
      <DataModeProvider>
        <FeaturesProvider>{children}</FeaturesProvider>
      </DataModeProvider>
    </QueryClientProvider>
  );
  return render(<PublicProfileScreen />, { wrapper });
}

describe("PublicProfileScreen", () => {
  it("renders identity from the fetched snapshot", async () => {
    renderScreen();
    expect(await screen.findByRole("heading", { name: SAMPLE_PROFILE.profile.name })).toBeTruthy();
    expect(screen.getByText(SAMPLE_PROFILE.profile.tagline)).toBeTruthy();
    expect(screen.getByText(SAMPLE_PROFILE.profile.initials)).toBeTruthy();
  });

  it("renders a season total", async () => {
    renderScreen();
    expect(await screen.findByText(SAMPLE_PROFILE.totals.laps.toLocaleString())).toBeTruthy();
  });

  it("renders one stamp per circuit with its lap count", async () => {
    renderScreen();
    await screen.findByRole("heading", { name: SAMPLE_PROFILE.profile.name });
    for (const t of SAMPLE_PROFILE.perTrack) {
      expect(screen.getByText(`${t.laps} laps`)).toBeTruthy();
    }
  });

  it("renders the records ledger with a PB marker", async () => {
    const { container } = renderScreen();
    await screen.findByRole("heading", { name: SAMPLE_PROFILE.profile.name });
    const root = within(container);
    for (const r of SAMPLE_PROFILE.records) {
      // PB rows show the same time as both season-best and all-time, so it can appear twice.
      expect(root.getAllByText(r.season).length).toBeGreaterThan(0);
    }
    expect(root.getAllByText("PB").length).toBeGreaterThan(0);
  });
});
