import { describe, expect, it } from "vitest";
import { buildHeatmap } from "./profile";

describe("buildHeatmap", () => {
  it("groups days into 7-cell week columns", () => {
    const weeks = buildHeatmap();
    expect(weeks.length).toBeGreaterThan(0);
    for (const week of weeks) {
      expect(week.length).toBe(7);
    }
  });

  it("pads the first week so day cells are Sunday-aligned", () => {
    // 2025-01-01 is a Wednesday (getDay() === 3) → first 3 cells are pad nulls.
    const weeks = buildHeatmap(new Date("2025-01-01T00:00:00Z"), new Date("2025-01-31T00:00:00Z"));
    const firstWeek = weeks[0]!;
    const firstReal = firstWeek.findIndex((d) => d !== null);
    expect(firstReal).toBe(firstWeek[firstReal]?.date.getDay());
  });

  it("assigns levels 0–4 by lap-count thresholds", () => {
    const weeks = buildHeatmap();
    const days = weeks.flat().filter((d): d is NonNullable<typeof d> => d !== null);
    for (const d of days) {
      expect(d.level).toBeGreaterThanOrEqual(0);
      expect(d.level).toBeLessThanOrEqual(4);
      if (d.laps === 0) expect(d.level).toBe(0);
      if (d.laps >= 34) expect(d.level).toBe(4);
    }
  });

  it("is deterministic for a given seed", () => {
    const a = buildHeatmap(undefined, undefined, 42);
    const b = buildHeatmap(undefined, undefined, 42);
    expect(JSON.stringify(a)).toBe(JSON.stringify(b));
  });
});
