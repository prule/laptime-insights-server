import type { BucketPlan } from "../providers/TimeRangeProvider";

const ONE_DAY_MS = 86_400_000;

/**
 * Build the N bucket start dates the dashboard chart layout uses, ending at the calendar bucket
 * containing `anchor`. Calendar-aligned: weeks start on Monday, months on the 1st. Returned in
 * chronological order. Joins onto aggregate-API responses by matching `bucketStartKey`.
 */
export function bucketStarts(plan: BucketPlan, anchor: number): Date[] {
  const anchorDate = new Date(anchor);
  const starts: Date[] = [];
  if (plan.unit === "week") {
    const dayIndex = (anchorDate.getDay() + 6) % 7; // 0 = Monday
    const anchorMonday = new Date(
      anchorDate.getFullYear(),
      anchorDate.getMonth(),
      anchorDate.getDate() - dayIndex,
    );
    for (let i = plan.count - 1; i >= 0; i--) {
      const start = new Date(anchorMonday);
      start.setDate(start.getDate() - i * 7);
      starts.push(start);
    }
  } else {
    const baseYear = anchorDate.getFullYear();
    const baseMonth = anchorDate.getMonth();
    for (let i = plan.count - 1; i >= 0; i--) {
      starts.push(new Date(baseYear, baseMonth - i, 1));
    }
  }
  return starts;
}

/**
 * Bar-chart x-axis labels. Kept short so they fit in the 3-column dashboard layout — full date
 * context is exposed on hover via the chart's `title` tooltip. Weeks show the day of month;
 * months show the 3-letter month name.
 */
export function bucketLabel(start: Date, unit: BucketPlan["unit"]): string {
  return unit === "week"
    ? start.toLocaleDateString("en-GB", { day: "2-digit" })
    : start.toLocaleDateString("en-GB", { month: "short" });
}

/**
 * Hover-tooltip text for a bucket: the full human-readable date so the reader can disambiguate
 * the terse on-screen label. Week buckets are always Monday-aligned so we omit the weekday name
 * (it would always read "Mon").
 */
export function bucketTitle(start: Date, unit: BucketPlan["unit"]): string {
  return unit === "week"
    ? `Week of ${start.toLocaleDateString("en-GB", {
        day: "2-digit",
        month: "short",
        year: "numeric",
      })}`
    : start.toLocaleDateString("en-GB", { month: "long", year: "numeric" });
}

/**
 * Aggregate-API bucket key — must stay in sync with the backend's `formatTimeBucketKey` in
 * `app/.../adapter/out/persistence/AggregationSupport.kt`. `YYYY-MM-DD` for week (Monday) and
 * day, `YYYY-MM` for month. Used to join the sparse aggregate response onto the dense
 * `bucketStarts` grid.
 */
export function bucketStartKey(start: Date, unit: BucketPlan["unit"]): string {
  const y = start.getFullYear();
  const m = String(start.getMonth() + 1).padStart(2, "0");
  if (unit === "month") return `${y}-${m}`;
  const d = String(start.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

/**
 * Parse a backend-emitted bucket key back into the calendar-aligned `Date` the key represents.
 * Used to find the latest data point in an aggregate response for "all"-range chart anchoring.
 */
export function parseBucketKey(key: string, unit: BucketPlan["unit"]): Date {
  if (unit === "month") {
    const [y, m] = key.split("-").map(Number);
    return new Date(y!, (m ?? 1) - 1, 1);
  }
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y!, (m ?? 1) - 1, d ?? 1);
}

/**
 * Return the latest bucket-start `Date` represented in `buckets`, or `null` if the list is empty.
 * Drives the "all"-range chart anchor so the rightmost bar hugs the data's actual end rather than
 * trailing into empty buckets up to today.
 */
export function latestBucketDate(
  buckets: readonly { key: string }[] | undefined,
  unit: BucketPlan["unit"],
): Date | null {
  if (!buckets || buckets.length === 0) return null;
  // `YYYY-MM-DD` / `YYYY-MM` sort lexicographically and chronologically.
  const latestKey = buckets.map((b) => b.key).sort()[buckets.length - 1]!;
  return parseBucketKey(latestKey, unit);
}

/**
 * Drop the sparse `buckets` from an aggregate endpoint onto the dense `bucketStarts` grid.
 * Missing buckets become `value: 0` so the chart renders gaps as empty bars. `getValue` selects
 * which metric to chart — e.g. `b => b.count` or `b => b.drivingTimeMs`.
 */
export function alignAggregate<B extends { key: string }>(
  buckets: readonly B[] | undefined,
  plan: BucketPlan,
  anchor: number,
  getValue: (b: B) => number,
): { label: string; title: string; value: number }[] {
  const lookup = new Map((buckets ?? []).map((b) => [b.key, getValue(b)]));
  return bucketStarts(plan, anchor).map((start) => ({
    label: bucketLabel(start, plan.unit),
    title: bucketTitle(start, plan.unit),
    value: lookup.get(bucketStartKey(start, plan.unit)) ?? 0,
  }));
}

export { ONE_DAY_MS };
