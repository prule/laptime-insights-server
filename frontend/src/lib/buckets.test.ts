import { describe, expect, it } from "vitest";
import {
  alignAggregate,
  bucketLabel,
  bucketStartKey,
  bucketStarts,
  bucketTitle,
  latestBucketDate,
  parseBucketKey,
} from "./buckets";

// Anchor used throughout — a Wednesday, far from a month/year boundary.
const ANCHOR = new Date(2026, 3, 8).getTime(); // 2026-04-08 (Wed)

/**
 * Format a Date as `YYYY-MM-DD` using local-TZ components. Tests must compare against this
 * rather than `toISOString().slice(0, 10)` because the bucket helpers build their Dates from
 * local-TZ constructors and ISO conversion shifts the day in non-UTC zones.
 */
function localISODate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${dd}`;
}

describe("bucketStarts", () => {
  it("walks back N Mondays from the anchor's week (weekly plan)", () => {
    const starts = bucketStarts({ unit: "week", count: 4 }, ANCHOR);
    expect(starts.map((d) => localISODate(d))).toEqual([
      "2026-03-16",
      "2026-03-23",
      "2026-03-30",
      "2026-04-06", // anchor's Monday
    ]);
  });

  it("includes the anchor day's own Monday as the rightmost week", () => {
    const wedAnchor = new Date(2026, 3, 8).getTime(); // Wed
    const monAnchor = new Date(2026, 3, 6).getTime(); // Mon
    const sunAnchor = new Date(2026, 3, 12).getTime(); // Sun (still same ISO week)
    for (const a of [wedAnchor, monAnchor, sunAnchor]) {
      const last = bucketStarts({ unit: "week", count: 1 }, a)[0];
      expect(localISODate(last!)).toBe("2026-04-06");
    }
  });

  it("walks back N month-starts from the anchor's month (monthly plan)", () => {
    const starts = bucketStarts({ unit: "month", count: 3 }, ANCHOR);
    expect(starts.map((d) => localISODate(d))).toEqual([
      "2026-02-01",
      "2026-03-01",
      "2026-04-01",
    ]);
  });
});

describe("bucketLabel", () => {
  it("renders day-of-month for weekly buckets", () => {
    expect(bucketLabel(new Date(2026, 3, 6), "week")).toBe("06");
  });
  it("renders 3-letter month for monthly buckets", () => {
    expect(bucketLabel(new Date(2026, 3, 1), "month")).toBe("Apr");
  });
});

describe("bucketTitle", () => {
  it("formats weekly tooltips as 'Week of dd Mon yyyy' without weekday", () => {
    expect(bucketTitle(new Date(2026, 3, 6), "week")).toBe("Week of 06 Apr 2026");
  });
  it("formats monthly tooltips as 'Month yyyy'", () => {
    expect(bucketTitle(new Date(2026, 3, 1), "month")).toBe("April 2026");
  });
});

describe("bucketStartKey ↔ parseBucketKey", () => {
  it("produces zero-padded YYYY-MM-DD keys for week buckets", () => {
    expect(bucketStartKey(new Date(2026, 3, 6), "week")).toBe("2026-04-06");
    // Pad single-digit day and month components.
    expect(bucketStartKey(new Date(2026, 0, 5), "week")).toBe("2026-01-05");
  });

  it("produces YYYY-MM keys for month", () => {
    expect(bucketStartKey(new Date(2026, 3, 1), "month")).toBe("2026-04");
  });

  it("round-trips through parseBucketKey for every supported unit", () => {
    const cases: { date: Date; unit: "week" | "month" }[] = [
      { date: new Date(2026, 3, 6), unit: "week" },
      { date: new Date(2026, 11, 28), unit: "week" },
      { date: new Date(2026, 0, 1), unit: "month" },
      { date: new Date(2026, 11, 1), unit: "month" },
    ];
    for (const { date, unit } of cases) {
      const key = bucketStartKey(date, unit);
      const parsed = parseBucketKey(key, unit);
      expect(parsed.getFullYear()).toBe(date.getFullYear());
      expect(parsed.getMonth()).toBe(date.getMonth());
      expect(parsed.getDate()).toBe(unit === "month" ? 1 : date.getDate());
    }
  });
});

describe("latestBucketDate", () => {
  it("returns null for empty / undefined input", () => {
    expect(latestBucketDate(undefined, "week")).toBeNull();
    expect(latestBucketDate([], "week")).toBeNull();
  });

  it("returns the calendar-aligned Date for the latest bucket key", () => {
    const result = latestBucketDate(
      [{ key: "2026-03-23" }, { key: "2026-04-06" }, { key: "2026-03-30" }],
      "week",
    );
    expect(result && localISODate(result)).toBe("2026-04-06");
  });
});

describe("alignAggregate", () => {
  it("fills missing buckets with zero", () => {
    const result = alignAggregate(
      [{ key: "2026-04-06", count: 5 }],
      { unit: "week", count: 3 },
      ANCHOR,
      (b) => b.count,
    );
    expect(result.map((r) => r.value)).toEqual([0, 0, 5]);
  });

  it("maps the sparse aggregate onto the dense bucketStarts grid in order", () => {
    const result = alignAggregate(
      [
        { key: "2026-03-23", count: 2 },
        { key: "2026-04-06", count: 7 },
      ],
      { unit: "week", count: 4 },
      ANCHOR,
      (b) => b.count,
    );
    expect(result.map((r) => r.value)).toEqual([0, 2, 0, 7]);
  });

  it("uses the supplied getValue selector (drivingTimeMs sample)", () => {
    const result = alignAggregate(
      [{ key: "2026-04", count: 3, drivingTimeMs: 1234 }],
      { unit: "month", count: 2 },
      ANCHOR,
      (b) => b.drivingTimeMs,
    );
    expect(result[1]!.value).toBe(1234);
    expect(result[0]!.value).toBe(0);
  });
});
