import { type ReactNode } from "react";
import { useProfileData } from "../api/profile";
import { LicenseCard } from "../components/profile/LicenseCard";
import { SeasonTotals } from "../components/profile/SeasonTotals";
import { ActivityHeatmap } from "../components/profile/ActivityHeatmap";
import { TrackStamps } from "../components/profile/TrackStamps";
import { RecordsLedger } from "../components/profile/RecordsLedger";
import { ErrorState, LoadingState } from "../components/ui/States";
import { formatDate } from "../lib/format";

/** Section header with the passport's title + sub + hairline flourish. */
function SecHead({ title, sub }: { title: string; sub?: ReactNode }) {
  return (
    <div className="mb-[18px] flex items-baseline gap-3">
      <h2 className="text-base font-semibold text-text">{title}</h2>
      {sub && <span className="font-mono text-[11px] text-text-muted">{sub}</span>}
      <span className="h-px flex-1 bg-border" />
    </div>
  );
}

/**
 * Public Profile — the "Driver Passport" concept. A shareable, premium vanity page rendered from
 * the snapshot the backend generates from local data (fetched via the `public-profile` HATEOAS
 * link). The route is only reachable when the feature is enabled, so an absent snapshot here is a
 * genuine load/error rather than a disabled profile.
 */
export function PublicProfileScreen() {
  const { data, isLoading, error, refetch } = useProfileData();

  if (isLoading) {
    return (
      <div className="p-8">
        <LoadingState />
      </div>
    );
  }
  if (error || !data) {
    return (
      <div className="p-8">
        <ErrorState error={error ?? new Error("No profile data")} onRetry={() => refetch()} />
      </div>
    );
  }

  const { profile, meta, totals, perTrack, records } = data;

  return (
    <div data-testid="screen-public-profile" className="h-full overflow-y-auto px-8 py-7">
      <div className="mx-auto max-w-[1080px]">
        {/* Header / share bar */}
        <div className="mb-7 flex items-center justify-between">
          <div>
            <div className="font-sans text-xl font-semibold text-text">Public Profile</div>
            <div className="font-sans text-[13px] text-text-muted">
              Your shareable driver passport · laptime.gg/{meta.slug}
            </div>
          </div>
          <div className="flex items-center gap-3">
            <span className="rounded-full border border-border px-[10px] py-[5px] font-mono text-[10px] tracking-[0.1em] text-text-muted">
              PUBLIC PROFILE
            </span>
            <button className="flex items-center gap-[7px] rounded-md border border-border-hover bg-surface-active px-[14px] py-[7px] font-mono text-[11px] tracking-[0.06em] text-text transition-colors hover:border-cyan hover:text-cyan">
              ⤴ Share
            </button>
          </div>
        </div>

        <LicenseCard data={data} />

        <section className="mt-11">
          <SecHead title="Season at a glance" sub={meta.range} />
          <SeasonTotals totals={totals} />
        </section>

        <section className="mt-11">
          <SecHead title="Time on track" sub={`${totals.longestStreak}-day longest streak`} />
          <ActivityHeatmap totals={totals} />
        </section>

        <section className="mt-11">
          <SecHead title="Laps per circuit" sub={`${totals.laps.toLocaleString()} laps total`} />
          <TrackStamps perTrack={perTrack} meta={meta} />
        </section>

        <section className="mt-11">
          <SecHead title="Personal records" sub="season best vs all-time" />
          <RecordsLedger records={records} />
        </section>

        <div className="mt-14 text-center">
          <div className="font-mono text-[10px] tracking-[0.08em] text-text-dim">
            Generated {formatDate(meta.generatedAt)} · {meta.sim} · updated automatically
          </div>
          <div className="mt-4 font-mono text-xs text-text-muted">
            laptime.gg/<b className="text-cyan">{profile.slug}</b>
          </div>
        </div>
      </div>
    </div>
  );
}
