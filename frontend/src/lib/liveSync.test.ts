import { describe, expect, it } from "vitest";
import { invalidationPrefixesFor, shouldConnect, LAP_PREFIXES, SESSION_PREFIXES } from "./liveSync";

describe("invalidationPrefixesFor", () => {
  it("invalidates lap-derived caches on LapCreated", () => {
    expect(invalidationPrefixesFor("LapCreated")).toEqual(LAP_PREFIXES);
  });

  it("invalidates session-derived caches on session lifecycle events", () => {
    for (const t of ["SessionCreated", "SessionStarted", "SessionUpdated", "SessionEnded"] as const) {
      expect(invalidationPrefixesFor(t)).toEqual(SESSION_PREFIXES);
    }
  });

  it("invalidates nothing for high-frequency telemetry / connection events", () => {
    expect(invalidationPrefixesFor("PlayerCarUpdated")).toEqual([]);
    expect(invalidationPrefixesFor("ServerStarted")).toEqual([]);
  });

  it("keeps lap and session prefix sets disjoint by first element", () => {
    // invalidateQueries matches on the first key element, so a lap event must not reach
    // the `sessions`/`sessions-aggregate` lists and vice versa.
    expect(LAP_PREFIXES).not.toContain("sessions");
    expect(SESSION_PREFIXES).not.toContain("laps");
  });
});

describe("shouldConnect", () => {
  it("connects only in live mode with a live rel", () => {
    expect(shouldConnect("live", "/api/1/events")).toBe(true);
  });

  it("does not connect in mock mode", () => {
    expect(shouldConnect("mock", "/api/1/events")).toBe(false);
  });

  it("does not connect when the live rel is absent", () => {
    expect(shouldConnect("live", undefined)).toBe(false);
  });
});
