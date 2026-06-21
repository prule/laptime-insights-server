import { ONE_DAY_MS } from "./time";

/**
 * Local-calendar key (YYYY-MM-DD) so two timestamps on the same day collapse to one streak entry
 * regardless of time-of-day. Uses local timezone — a session that starts late at night counts on
 * the day the player perceives it as.
 */
function dayKey(ms: number): string {
  const d = new Date(ms);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
}

function parseDayKey(key: string): Date {
  const [y, m, d] = key.split("-").map(Number);
  return new Date(y!, (m ?? 1) - 1, d ?? 1);
}

/**
 * Number of consecutive calendar days ending at the most recent session day. `lastDate` is the
 * most recent day with a session (or null when there are no sessions). The header decides whether
 * the streak is "live" (last day is today or yesterday) or "ended" based on `lastDate`.
 */
export function computeStreak(timestamps: string[]): {
  days: number;
  lastDate: Date | null;
} {
  return streakFromDayKeys(timestamps.map((t) => dayKey(new Date(t).getTime())));
}

/**
 * Streak length from a set of active-day keys (`YYYY-MM-DD`), e.g. the keys of the server's
 * all-time daily session aggregate. Duplicates are collapsed, so callers can pass aggregate
 * buckets directly. Same return contract as {@link computeStreak}.
 */
export function streakFromDayKeys(dayKeys: string[]): {
  days: number;
  lastDate: Date | null;
} {
  if (dayKeys.length === 0) return { days: 0, lastDate: null };
  const keys = Array.from(new Set(dayKeys)).sort();
  const desc = keys.slice().reverse();
  const lastDate = parseDayKey(desc[0]!);
  let streak = 1;
  let prev = lastDate;
  for (let i = 1; i < desc.length; i++) {
    const cur = parseDayKey(desc[i]!);
    const diffDays = Math.round((prev.getTime() - cur.getTime()) / ONE_DAY_MS);
    if (diffDays === 1) {
      streak++;
      prev = cur;
    } else {
      break;
    }
  }
  return { days: streak, lastDate };
}

/**
 * Pretty-print a streak's freshness for the header badge. `live` = the streak ticked today or
 * yesterday (still recoverable); past that it's broken.
 */
export function describeStreakFreshness(
  lastDate: Date | null,
  now: Date = new Date(),
): { live: boolean; lastLabel: string | null } {
  if (!lastDate) return { live: false, lastLabel: null };
  const todayMs = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const diffDays = Math.round((todayMs - lastDate.getTime()) / ONE_DAY_MS);
  const live = diffDays <= 1;
  const lastLabel =
    diffDays === 0
      ? "today"
      : diffDays === 1
        ? "yesterday"
        : `ended ${lastDate.toLocaleDateString("en-GB", { day: "2-digit", month: "short" })}`;
  return { live, lastLabel };
}
