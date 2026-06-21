import { describe, expect, it } from "vitest";
import { computeStreak, describeStreakFreshness, streakFromDayKeys } from "./streak";

/** Helper: produce an ISO instant at local noon for the given y/m/d (1-based month). */
function isoLocalNoon(year: number, month: number, day: number): string {
  return new Date(year, month - 1, day, 12, 0, 0).toISOString();
}

describe("computeStreak", () => {
  it("returns 0 / null for no timestamps", () => {
    expect(computeStreak([])).toEqual({ days: 0, lastDate: null });
  });

  it("counts a single session as a 1-day streak", () => {
    const ts = isoLocalNoon(2026, 4, 8);
    const { days, lastDate } = computeStreak([ts]);
    expect(days).toBe(1);
    expect(lastDate?.toDateString()).toBe(new Date(2026, 3, 8).toDateString());
  });

  it("collapses multiple sessions on the same day into one streak day", () => {
    const ts1 = isoLocalNoon(2026, 4, 8);
    const ts2 = new Date(2026, 3, 8, 22, 0, 0).toISOString();
    expect(computeStreak([ts1, ts2]).days).toBe(1);
  });

  it("counts consecutive days backwards from the most recent session", () => {
    const days = [
      isoLocalNoon(2026, 4, 6),
      isoLocalNoon(2026, 4, 7),
      isoLocalNoon(2026, 4, 8),
    ];
    expect(computeStreak(days).days).toBe(3);
  });

  it("stops counting when a gap appears", () => {
    const ts = [
      isoLocalNoon(2026, 4, 2),
      isoLocalNoon(2026, 4, 3),
      // gap on 04-04
      isoLocalNoon(2026, 4, 5),
      isoLocalNoon(2026, 4, 6),
    ];
    // Backwards from 04-06: 04-06, 04-05 are consecutive → streak = 2.
    expect(computeStreak(ts).days).toBe(2);
  });
});

describe("streakFromDayKeys", () => {
  it("returns 0 / null for no active days", () => {
    expect(streakFromDayKeys([])).toEqual({ days: 0, lastDate: null });
  });

  it("counts consecutive active-day keys backwards from the most recent", () => {
    const { days, lastDate } = streakFromDayKeys(["2026-04-06", "2026-04-07", "2026-04-08"]);
    expect(days).toBe(3);
    expect(lastDate?.toDateString()).toBe(new Date(2026, 3, 8).toDateString());
  });

  it("stops at the first gap and tolerates unsorted / duplicate keys", () => {
    const { days } = streakFromDayKeys([
      "2026-04-06",
      "2026-04-02",
      "2026-04-05",
      "2026-04-06", // duplicate
      "2026-04-03",
      // gap on 04-04
    ]);
    expect(days).toBe(2); // 04-06, 04-05
  });
});

describe("describeStreakFreshness", () => {
  // Pin "now" so tests are deterministic.
  const now = new Date(2026, 3, 10, 9, 0, 0); // 2026-04-10
  const today = new Date(2026, 3, 10);
  const yesterday = new Date(2026, 3, 9);
  const fourDaysAgo = new Date(2026, 3, 6);

  it("reads 'today' and is live when the last day is today", () => {
    expect(describeStreakFreshness(today, now)).toEqual({ live: true, lastLabel: "today" });
  });

  it("reads 'yesterday' and is still live when the last day was yesterday", () => {
    expect(describeStreakFreshness(yesterday, now)).toEqual({
      live: true,
      lastLabel: "yesterday",
    });
  });

  it("reads 'ended dd Mon' and is no longer live past one day", () => {
    const { live, lastLabel } = describeStreakFreshness(fourDaysAgo, now);
    expect(live).toBe(false);
    expect(lastLabel).toBe("ended 06 Apr");
  });

  it("returns null label when there's no streak at all", () => {
    expect(describeStreakFreshness(null, now)).toEqual({ live: false, lastLabel: null });
  });
});
