// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter, useLocation } from "react-router-dom";
import { CompareScreen } from "./CompareScreen";
import type { LapResource, SessionResource } from "../api/types";

/**
 * Controllable in-memory dataset the mocked `../api/queries` hooks read from.
 * `vi.hoisted` lets the (hoisted) mock factory below close over it.
 */
const env = vi.hoisted(() => {
  const sessions: SessionResource[] = [];
  const laps: LapResource[] = [];
  return { sessions, laps };
});

function lap(p: Partial<LapResource> & { uid: string }): LapResource {
  return {
    uid: p.uid,
    sessionUid: p.sessionUid ?? "s1",
    carId: p.carId ?? 1,
    car: p.car ?? "Ferrari 296",
    track: p.track ?? "Spa",
    playerLap: p.playerLap ?? false,
    recordedAt: p.recordedAt ?? "2026-06-20T10:00:00Z",
    lapTime: p.lapTime ?? 100000,
    lapNumber: p.lapNumber ?? 1,
    valid: p.valid ?? true,
    personalBest: p.personalBest ?? false,
    _links: { telemetry: `/t/${p.uid}` },
  };
}

function session(p: Partial<SessionResource> & { uid: string }): SessionResource {
  return {
    uid: p.uid,
    startedAt: p.startedAt ?? "2026-06-20T09:00:00Z",
    endedAt: p.endedAt ?? "2026-06-20T11:00:00Z",
    simulator: p.simulator ?? "ACC",
    track: p.track ?? "Spa",
    car: p.car ?? "Ferrari 296",
    sessionType: p.sessionType ?? "Practice",
    playerCarId: p.playerCarId ?? 1,
    drivingTimeMs: p.drivingTimeMs ?? 0,
    _links: { laps: `/sessions/${p.uid}/laps` },
  };
}

const ok = (data: unknown) => ({ data, isLoading: false, isError: false, error: null, refetch: vi.fn() });

vi.mock("../providers/FeaturesProvider", () => ({
  useFeatureEnabled: () => true,
  useFeatures: () => ({ links: {} }),
}));

vi.mock("../api/queries", () => ({
  useSessions: () => ok({ items: env.sessions, total: env.sessions.length, page: { page: 1, size: 25 } }),
  useSessionOptions: () =>
    ok({ tracks: ["Spa", "Monza"], cars: ["Ferrari 296", "Audi R8"], simulators: ["ACC"] }),
  useSessionLaps: (s?: SessionResource) =>
    ok(
      s
        ? { items: env.laps.filter((l) => l.sessionUid === s.uid), total: 0, page: { page: 1, size: 200 } }
        : undefined,
    ),
  useTrackBestLap: (track?: string | null) =>
    ok(
      track
        ? [...env.laps.filter((l) => l.valid && l.track === track)].sort((a, b) => a.lapTime - b.lapTime)[0] ??
            null
        : null,
    ),
  useLaps: (params: { track?: string; car?: string; playerLap?: boolean; page?: number; size?: number }) => {
    const items = env.laps
      .filter((l) => (params.track ? l.track === params.track : true))
      .filter((l) => (params.car ? l.car === params.car : true))
      .filter((l) => (params.playerLap ? l.playerLap === true : true))
      .filter((l) => l.valid)
      .sort((a, b) => a.lapTime - b.lapTime);
    return ok({ items, total: items.length, page: { page: params.page ?? 1, size: params.size ?? 50 } });
  },
  useLap: (uid?: string) => ok(uid ? env.laps.find((l) => l.uid === uid) ?? undefined : undefined),
  useSession: (uid?: string) => ok(uid ? env.sessions.find((s) => s.uid === uid) ?? undefined : undefined),
  useLapComparison: (a?: string, b?: string) => {
    if (!a || !b) return ok(undefined);
    const la = env.laps.find((l) => l.uid === a)!;
    const lb = env.laps.find((l) => l.uid === b)!;
    const side = (l: LapResource) => ({
      lapUid: l.uid,
      sessionUid: l.sessionUid,
      lapNumber: l.lapNumber,
      lapTimeMs: l.lapTime,
      valid: l.valid,
      personalBest: l.personalBest,
      samples: [],
    });
    return ok({ lap1: side(la), lap2: side(lb), _links: {} });
  },
}));

// The trace/map panels need no real rendering for these tests.
vi.mock("../components/ui/TelemetryTrace", () => ({ TelemetryTrace: () => <div data-testid="telemetry" /> }));
vi.mock("../components/ui/TrackMap", () => ({ TrackMap: () => <div data-testid="trackmap" /> }));
vi.mock("../components/ui/SpeedDeltaTrace", () => ({ SpeedDeltaTrace: () => <div data-testid="delta" /> }));
vi.mock("../components/ui/GearMismatchStrip", () => ({ GearMismatchStrip: () => <div data-testid="gear" /> }));

function LocationProbe() {
  const loc = useLocation();
  return <div data-testid="search">{loc.search}</div>;
}

function renderAt(initialEntry: string) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <CompareScreen />
      <LocationProbe />
    </MemoryRouter>,
  );
}

function search() {
  return screen.getByTestId("search").textContent ?? "";
}

beforeEach(() => {
  env.sessions = [session({ uid: "s1", track: "Spa", car: "Ferrari 296" })];
  env.laps = [
    lap({ uid: "mine-fast", sessionUid: "s1", playerLap: true, lapTime: 107231, lapNumber: 5 }),
    lap({ uid: "mine-slow", sessionUid: "s1", playerLap: true, lapTime: 107900, lapNumber: 6 }),
    lap({ uid: "rival", sessionUid: "s1", playerLap: false, lapTime: 108000, lapNumber: 4, car: "Audi R8" }),
  ];
});

afterEach(() => cleanup());

describe("CompareScreen seeding", () => {
  it("seeds the track and the player's fastest lap as the anchor on a fresh landing", () => {
    renderAt("/compare");
    // Track select defaults to the latest session's track.
    expect((screen.getByRole("combobox") as HTMLSelectElement).value).toBe("Spa");
    // Anchor summary shows the player's fastest lap time (1:47.231) — also in the leaderboard row.
    expect(screen.getAllByText(/1:47\.231/).length).toBeGreaterThan(0);
    expect(screen.getByText(/· auto/)).toBeTruthy();
  });

  it("falls back to the session best when the player has no lap in the session", () => {
    env.laps = [
      lap({ uid: "rival-fast", sessionUid: "s1", playerLap: false, lapTime: 108000 }),
      lap({ uid: "rival-slow", sessionUid: "s1", playerLap: false, lapTime: 109000 }),
    ];
    renderAt("/compare");
    // 1:48.000 = the session best (fastest valid lap by anyone) — anchor + leaderboard row.
    expect(screen.getAllByText(/1:48\.000/).length).toBeGreaterThan(0);
  });

  it("defaults the leaderboard to Field when the player is not identifiable", () => {
    env.laps = [lap({ uid: "rival-fast", sessionUid: "s1", playerLap: false, lapTime: 108000 })];
    renderAt("/compare");
    // With no player lap, the anchor falls back to the field best and the leaderboard shows it
    // (would be empty under a "Me" default since playerLap is never true here).
    const seg = screen.getByText("Field").closest("button");
    expect(seg?.className).toMatch(/cyan/);
  });
});

describe("CompareScreen track axis", () => {
  it("clears anchor and challenger when the track changes", () => {
    renderAt("/compare?track=Spa&anchor=mine-fast&challenger=mine-slow");
    fireEvent.change(screen.getByRole("combobox"), { target: { value: "Monza" } });
    const s = search();
    expect(s).toContain("track=Monza");
    expect(s).not.toContain("anchor=");
    expect(s).not.toContain("challenger=");
  });
});

describe("CompareScreen challenger leaderboard", () => {
  it("ranks laps fastest-first and picking a row sets the challenger and renders the comparison", () => {
    renderAt("/compare?track=Spa&anchor=mine-fast");
    // Pick the 2nd fastest lap as challenger.
    fireEvent.click(screen.getByText(/1:47\.900/));
    expect(search()).toContain("challenger=mine-slow");
    // Comparison panels now render (both anchor + challenger resolved).
    expect(screen.getByTestId("telemetry")).toBeTruthy();
    expect(screen.getAllByText(/Challenger/).length).toBeGreaterThan(0);
  });
});

describe("CompareScreen back-compat", () => {
  it("honors old lap1/lap2 links as anchor/challenger", () => {
    renderAt("/compare?track=Spa&lap1=mine-fast&lap2=rival");
    // Comparison renders from the legacy params without any anchor/challenger keys.
    expect(screen.getByTestId("delta")).toBeTruthy();
    expect(screen.getByTestId("gear")).toBeTruthy();
  });
});
