import { useMemo, useState } from "react";
import { useLap, useLapComparison, useSessionOptions } from "../api/queries";
import { Card } from "../components/ui/Card";
import { ErrorState, LoadingState, EmptyState } from "../components/ui/States";
import { GearTrace } from "../components/ui/GearTrace";
import { AnchorControl } from "../components/AnchorControl";
import { LapLeaderboard } from "../components/LapLeaderboard";
import { FilterSelect } from "../components/ui/FilterSelect";
import { SectionHeader } from "../components/ui/SectionHeader";
import { SpeedDeltaTrace } from "../components/ui/SpeedDeltaTrace";
import { TelemetryTrace } from "../components/ui/TelemetryTrace";
import { TrackMap } from "../components/ui/TrackMap";
import { useCompareSeed } from "../hooks/useCompareSeed";
import { formatLapTime } from "../lib/format";
import { getString, useUrlState } from "../hooks/useUrlState";

const COLOR_ANCHOR = "#00d4ff";
const COLOR_CHALLENGER = "#e8212a";

/**
 * Lap-comparison screen.
 *
 * Track is the comparison axis: chosen once and shared by both laps, so a
 * comparison can never mix tracks. URL state owns `track`, `anchor`, `challenger`
 * (reload-safe + shareable). For backward compatibility with old links, `lap1`/
 * `lap2` are read as `anchor`/`challenger`.
 *
 * On a fresh landing (no `track`/`anchor`) the screen seeds itself from the
 * latest session via `useCompareSeed`: that session's track becomes the axis and
 * its default lap (the player's fastest, else the session best) becomes the
 * anchor — so the screen is useful with zero clicks. The anchor is the reference
 * point; the challenger is swept from a ranked same-track leaderboard.
 *
 * hoveredPosition (0–1 splinePosition) is lifted here so all panels stay
 * synchronized as the user hovers over any one of them.
 */
export function CompareScreen() {
  const [params, setParam, setMany] = useUrlState();

  const trackParam = getString(params, "track");
  // Back-compat: honor old `lap1`/`lap2` links as anchor/challenger.
  const anchorParam = getString(params, "anchor") ?? getString(params, "lap1");
  const challengerParam = getString(params, "challenger") ?? getString(params, "lap2");

  const optionsQuery = useSessionOptions();
  const seed = useCompareSeed(trackParam);

  // Effective axis/anchor: explicit param wins, else the latest-session seed.
  const track = trackParam ?? seed.seedTrack;
  const anchorUid = anchorParam ?? seed.defaultAnchor?.uid;
  const isDefaultAnchor = !anchorParam && !!seed.defaultAnchor;

  const anchorLapQuery = useLap(anchorUid);
  const anchorLap = anchorLapQuery.data;
  const seedCar = anchorLap?.car ?? seed.seedCar;
  // Only default the leaderboard to "Me" when the player is actually identifiable; otherwise the
  // "Me" filter (playerLap=true) yields an empty list against data where playerLap is null.
  const defaultDriver = anchorLap?.playerLap === true ? "me" : "field";

  const comparisonQuery = useLapComparison(anchorUid, challengerParam);

  const [hoveredPosition, setHoveredPosition] = useState<number | null>(null);

  const series = useMemo(() => {
    if (!comparisonQuery.data) return [];
    return [
      { samples: comparisonQuery.data.lap1.samples, color: COLOR_ANCHOR, label: "Anchor" },
      { samples: comparisonQuery.data.lap2.samples, color: COLOR_CHALLENGER, label: "Challenger" },
    ];
  }, [comparisonQuery.data]);

  // Changing the track is a hard reset of incompatible selections (same-track axis).
  const onTrackChange = (next: string | undefined) => {
    setMany({ track: next, anchor: undefined, challenger: undefined, lap1: undefined, lap2: undefined });
  };

  const onPickAnchor = (lap: { uid: string }) => setParam("anchor", lap.uid);
  const onPickChallenger = (lap: { uid: string }) => setParam("challenger", lap.uid);

  const ready = !!track && !!anchorUid && !!challengerParam;

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      <Card className="mb-4">
        <SectionHeader
          title="Pick laps to compare"
          sub="Track is the shared axis. The anchor seeds from your latest session; sweep the ranked leaderboard for a challenger. URL stays in sync so the comparison is shareable."
        />
        <div className="flex flex-wrap items-end gap-3">
          <FilterSelect
            label="Track"
            value={track}
            options={optionsQuery.data?.tracks ?? []}
            onChange={onTrackChange}
          />
          <AnchorControl
            track={track}
            anchorLap={anchorLap}
            seedCar={seedCar}
            scopeSession={seed.latestSession}
            isDefault={isDefaultAnchor}
            accentColor={COLOR_ANCHOR}
            onChange={onPickAnchor}
          />
          {(trackParam || anchorParam || challengerParam) && (
            <button
              onClick={() =>
                setMany({
                  track: undefined,
                  anchor: undefined,
                  challenger: undefined,
                  lap1: undefined,
                  lap2: undefined,
                })
              }
              className="self-end rounded border border-border px-3 py-2 font-mono text-[11px] uppercase tracking-[0.08em] text-text-muted hover:text-text"
            >
              Reset
            </button>
          )}
        </div>
      </Card>

      {!track ? (
        <Card>
          <EmptyState
            title="No track selected"
            description="Pick a track above — or record a session and we'll seed this from your latest one."
          />
        </Card>
      ) : (
        <Card className="mb-4">
          <SectionHeader
            title="Challenger"
            sub="Ranked fastest-first at this track. Toggle scope, driver, and same-car; click a row to compare it against the anchor."
          />
          {anchorUid && anchorLapQuery.isLoading ? (
            <LoadingState />
          ) : (
            // Mount only once the anchor has resolved: the leaderboard's initial driver toggle is
            // derived from the anchor's `playerLap`, and the inner component reads it only at mount.
            <LapLeaderboard
              track={track}
              seedCar={seedCar}
              scopeSession={seed.latestSession}
              defaultDriver={defaultDriver}
              anchorLapUid={anchorUid}
              selectedLapUid={challengerParam}
              onPick={onPickChallenger}
            />
          )}
        </Card>
      )}

      {!ready ? null : comparisonQuery.isLoading ? (
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
                color={COLOR_ANCHOR}
                label="Anchor"
                lapNumber={comparisonQuery.data.lap1.lapNumber}
                lapTimeMs={comparisonQuery.data.lap1.lapTimeMs}
                personalBest={comparisonQuery.data.lap1.personalBest}
              />
              <LapHeader
                color={COLOR_CHALLENGER}
                label="Challenger"
                lapNumber={comparisonQuery.data.lap2.lapNumber}
                lapTimeMs={comparisonQuery.data.lap2.lapTimeMs}
                personalBest={comparisonQuery.data.lap2.personalBest}
              />
            </div>
          </Card>

          {/* Track map + speed chart side-by-side at the top */}
          <div className="mb-4 flex gap-4">
            <Card className="flex flex-1 flex-col">
              <SectionHeader title="Speed (KPH)" sub="Both laps overlaid against splinePosition (0 → 1)" />
              <TelemetryTrace
                series={series}
                field="speedKph"
                height={180}
                unit="kph"
                hoveredPosition={hoveredPosition}
                onHover={setHoveredPosition}
              />
            </Card>
            <Card className="flex flex-col items-center justify-center">
              <SectionHeader title="Track map" sub="Hover any chart to see position" />
              <TrackMap
                series={series}
                hoveredPosition={hoveredPosition}
                onHover={setHoveredPosition}
                size={220}
              />
            </Card>
          </div>

          <Card className="mb-4">
            <SectionHeader title="Speed delta" sub="Anchor minus challenger at every 1% of track length" />
            <SpeedDeltaTrace
              lap1={comparisonQuery.data.lap1.samples}
              lap2={comparisonQuery.data.lap2.samples}
              hoveredPosition={hoveredPosition}
              onHover={setHoveredPosition}
            />
          </Card>

          <Card className="mb-4">
            <SectionHeader title="Gear" sub="Both laps' gear against track position; red shading marks where they differ" />
            <GearTrace
              series={series}
              hoveredPosition={hoveredPosition}
              onHover={setHoveredPosition}
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
