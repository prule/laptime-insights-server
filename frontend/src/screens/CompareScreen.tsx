import { useMemo } from "react";
import { useLapComparison } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { GearMismatchStrip } from "../components/ui/GearMismatchStrip";
import { LapPicker } from "../components/LapPicker";
import { SectionHeader } from "../components/ui/SectionHeader";
import { SpeedDeltaTrace } from "../components/ui/SpeedDeltaTrace";
import { TelemetryTrace } from "../components/ui/TelemetryTrace";
import { formatLapTime } from "../lib/format";
import { getString, useUrlState } from "../hooks/useUrlState";

const COLOR_LAP1 = "#00d4ff";
const COLOR_LAP2 = "#e8212a";

/**
 * Lap-comparison screen.
 *
 * URL state owns `track`, `lap1`, `lap2`. Reload-safe and shareable.
 *
 * Each lap slot is a `LapPicker` — a button that pops a modal with the same
 * track/car/PB filters and pagination as the Laps screen. The optional
 * `track` URL param pre-fills the picker's track filter so jumps from
 * SessionDetail's "vs best" / "vs PB" buttons land on the right context.
 */
export function CompareScreen() {
  const [params, setParam, setMany] = useUrlState();

  const track = getString(params, "track");
  const lap1Uid = getString(params, "lap1");
  const lap2Uid = getString(params, "lap2");

  const comparisonQuery = useLapComparison(lap1Uid, lap2Uid);

  const series = useMemo(() => {
    if (!comparisonQuery.data) return [];
    return [
      { samples: comparisonQuery.data.lap1.samples, color: COLOR_LAP1, label: "Lap 1" },
      { samples: comparisonQuery.data.lap2.samples, color: COLOR_LAP2, label: "Lap 2" },
    ];
  }, [comparisonQuery.data]);

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader
          title="Pick laps to compare"
          sub="Both pickers open a searchable list — filter by track/car, paginate, click to pick. URL stays in sync so the comparison is shareable."
        />
        <div className="flex flex-wrap items-end gap-3">
          <LapPicker
            label="Lap 1"
            accentColor={COLOR_LAP1}
            defaultTrack={track}
            selectedUid={lap1Uid}
            disabledLapUid={lap2Uid}
            onSelect={(v) => setParam("lap1", v)}
          />
          <LapPicker
            label="Lap 2"
            accentColor={COLOR_LAP2}
            defaultTrack={track}
            selectedUid={lap2Uid}
            disabledLapUid={lap1Uid}
            onSelect={(v) => setParam("lap2", v)}
          />
          {(lap1Uid || lap2Uid || track) && (
            <button
              onClick={() => setMany({ track: undefined, lap1: undefined, lap2: undefined })}
              className="self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
            >
              Reset
            </button>
          )}
        </div>
      </Card>

      {!lap1Uid || !lap2Uid ? (
        <Card>
          <EmptyState
            title="Pick two laps"
            description="Use the pickers above. Tip: from a session detail page, the per-row 'vs best' or 'vs PB' buttons land here with both laps preselected."
          />
        </Card>
      ) : comparisonQuery.isLoading ? (
        <Card>
          <LoadingState />
        </Card>
      ) : comparisonQuery.isError ? (
        <Card>
          <ErrorState error={comparisonQuery.error} onRetry={() => comparisonQuery.refetch()} />
        </Card>
      ) : comparisonQuery.data ? (
        <>
          <Card className="mb-4">
            <div className="grid grid-cols-2 gap-6">
              <LapHeader
                color={COLOR_LAP1}
                label="Lap 1"
                lapNumber={comparisonQuery.data.lap1.lapNumber}
                lapTimeMs={comparisonQuery.data.lap1.lapTimeMs}
                personalBest={comparisonQuery.data.lap1.personalBest}
              />
              <LapHeader
                color={COLOR_LAP2}
                label="Lap 2"
                lapNumber={comparisonQuery.data.lap2.lapNumber}
                lapTimeMs={comparisonQuery.data.lap2.lapTimeMs}
                personalBest={comparisonQuery.data.lap2.personalBest}
              />
            </div>
          </Card>

          <Card className="mb-4">
            <SectionHeader title="Speed (KPH)" sub="Both laps overlaid against splinePosition (0 → 1)" />
            <TelemetryTrace series={series} field="speedKph" height={180} unit="kph" />
          </Card>

          <Card className="mb-4">
            <SectionHeader title="Speed delta" sub="Lap 1 minus Lap 2 at every 1% of track length" />
            <SpeedDeltaTrace
              lap1={comparisonQuery.data.lap1.samples}
              lap2={comparisonQuery.data.lap2.samples}
            />
          </Card>

          <Card className="mb-4">
            <SectionHeader title="Gear mismatch" sub="Red strips mark sectors where the two laps used different gears" />
            <GearMismatchStrip
              lap1={comparisonQuery.data.lap1.samples}
              lap2={comparisonQuery.data.lap2.samples}
            />
          </Card>

        </>
      ) : null}
    </div>
  );
}

function LapHeader({
  color,
  label,
  lapNumber,
  lapTimeMs,
  personalBest,
}: {
  color: string;
  label: string;
  lapNumber: number;
  lapTimeMs: number;
  personalBest: boolean;
}) {
  return (
    <div className="flex items-center gap-4">
      <div className="h-3 w-3 rounded-full" style={{ background: color }} />
      <div>
        <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted">
          {label} {personalBest && <span className="text-ok">· PB</span>}
        </div>
        <div className="font-mono text-xl text-text">{formatLapTime(lapTimeMs)}</div>
        <div className="font-sans text-xs text-text-muted">Lap #{lapNumber}</div>
      </div>
    </div>
  );
}
