import { useMemo } from "react";
import { buildHeatmap, type HeatmapWeek, type Totals } from "../../api/profile";

const CELL = "h-[13px] w-[13px] rounded-[3px]";

const LEVEL_STYLE: Record<number, string> = {
  0: "bg-white/[0.04]",
  1: "bg-cyan/25",
  2: "bg-cyan/45",
  3: "bg-cyan/70",
  4: "bg-cyan shadow-[0_0_6px_rgba(0,212,255,0.6)]",
};

/** Month label for each week column (only the first column of each new month is labelled). */
function monthLabels(weeks: HeatmapWeek[]): (string | null)[] {
  const labels: (string | null)[] = [];
  let lastMonth = -1;
  for (const week of weeks) {
    const first = week.find((d) => d);
    if (first) {
      const m = first.date.getMonth();
      if (m !== lastMonth) {
        labels.push(first.date.toLocaleString("en", { month: "short" }));
        lastMonth = m;
      } else {
        labels.push(null);
      }
    } else {
      labels.push(null);
    }
  }
  return labels;
}

/** GitHub-style activity heatmap with month labels, legend, and streak/active-days summary. */
export function ActivityHeatmap({ totals }: { totals: Totals }) {
  const weeks = useMemo(() => buildHeatmap(), []);
  const months = useMemo(() => monthLabels(weeks), [weeks]);

  return (
    <div className="rounded-lg border border-border bg-surface p-[22px]">
      <div className="mb-2 flex gap-[3px] font-mono text-[9px] text-text-dim">
        {months.map((label, i) => (
          <div key={i} className="w-[13px]">
            {i > 0 ? (label ?? "") : ""}
          </div>
        ))}
      </div>

      <div className="overflow-x-auto pb-[6px]">
        <div className="flex gap-[3px]">
          {weeks.map((week, wi) => (
            <div key={wi} className="flex flex-col gap-[3px]">
              {week.map((day, di) =>
                day ? (
                  <div
                    key={di}
                    className={`${CELL} ${LEVEL_STYLE[day.level]}`}
                    title={`${day.date.toDateString()} · ${day.laps} laps`}
                  />
                ) : (
                  <div key={di} className={`${CELL} invisible`} />
                ),
              )}
            </div>
          ))}
        </div>
      </div>

      <div className="mt-[14px] flex items-center gap-[14px] font-mono text-[10px] text-text-muted">
        <span>{totals.daysActive} active days this season</span>
        <div className="flex-1" />
        <div className="flex items-center gap-[3px]">
          Less
          <span className={`${CELL} ${LEVEL_STYLE[0]}`} />
          <span className={`${CELL} ${LEVEL_STYLE[1]}`} />
          <span className={`${CELL} ${LEVEL_STYLE[2]}`} />
          <span className={`${CELL} ${LEVEL_STYLE[3]}`} />
          <span className={`${CELL} ${LEVEL_STYLE[4]}`} />
          More
        </div>
      </div>
    </div>
  );
}
