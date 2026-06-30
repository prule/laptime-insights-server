import type { ProfileData } from "../../api/profile";

/** Build the machine-readable (MRZ) flourish strip along the bottom of the license. */
function mrzLine(data: ProfileData): string {
  const { profile, meta, totals } = data;
  const name = profile.name.toUpperCase().replace(/ /g, "<");
  const slug = meta.slug.toUpperCase().replace(/-/g, "<");
  const year = meta.season.replace(/\D/g, "");
  return `LTI<<${name}<<${slug}<<${year}<<${totals.laps}LAPS<<${totals.distanceKm}KM<<${totals.cars}CARS<<${"<".repeat(14)}`;
}

interface FieldProps {
  label: string;
  value: string | number;
}

function Field({ label, value }: FieldProps) {
  return (
    <div>
      <div className="font-mono text-[9px] uppercase tracking-[0.14em] text-text-dim">{label}</div>
      <div className="mt-[3px] text-[13px] text-text">{value}</div>
    </div>
  );
}

/**
 * Credential-style identity card. Decorative passport chrome (holographic foil strip, guilloché
 * security pattern, gold chip, MRZ strip) is intentionally kept as inline/arbitrary styles — it has
 * no design-token equivalent. Layout/semantic colours use the app's Tailwind tokens.
 */
export function LicenseCard({ data }: { data: ProfileData }) {
  const { profile, meta, totals } = data;

  return (
    <div
      className="relative overflow-hidden rounded-[18px] border border-border-hover px-[34px] py-[30px] shadow-[0_30px_80px_rgba(0,0,0,0.55),inset_0_1px_0_rgba(255,255,255,0.05)]"
      style={{ background: "linear-gradient(135deg, #11151d, #0b0e14 60%)" }}
    >
      {/* guilloché-ish security pattern */}
      <div
        className="pointer-events-none absolute inset-0 opacity-50"
        style={{
          backgroundImage:
            "repeating-radial-gradient(circle at 80% 30%, rgba(0,212,255,0.04) 0 1px, transparent 1px 7px), repeating-linear-gradient(115deg, rgba(255,255,255,0.015) 0 2px, transparent 2px 9px)",
        }}
      />
      {/* holographic foil strip */}
      <div
        className="absolute bottom-0 right-0 top-0 w-[6px] opacity-85"
        style={{
          background:
            "linear-gradient(180deg, var(--lt-foil-1, #00d4ff), #e8212a, #f97316, #00d4ff)",
        }}
      />

      <div className="relative z-[1] flex items-center justify-between">
        <div className="font-mono text-[10px] tracking-[0.22em] text-text-muted">DRIVER PROFILE</div>
        <div className="rounded-full border border-cyan/30 bg-cyan/10 px-[10px] py-1 font-mono text-[11px] tracking-[0.1em] text-cyan">
          {meta.season.toUpperCase()}
        </div>
      </div>

      <div className="relative z-[1] mt-[22px] grid grid-cols-[auto_1fr_auto] items-center gap-7">
        <div className="flex h-[120px] w-[120px] flex-col items-center justify-center rounded-2xl border border-cyan/30 bg-gradient-to-br from-cyan/[0.18] to-cyan/[0.04]">
          <b className="font-mono text-[44px] font-bold leading-none tracking-[0.02em] text-cyan [text-shadow:0_0_30px_rgba(0,212,255,0.4)]">
            {profile.initials}
          </b>
          <span className="mt-2 font-mono text-[9px] tracking-[0.16em] text-text-muted">DRIVER</span>
        </div>

        <div>
          <h1 className="text-[34px] font-bold tracking-[-0.01em] text-text">{profile.name}</h1>
          <div className="mt-[6px] font-mono text-xs tracking-[0.02em] text-cyan">
            {profile.tagline}
          </div>
          <div className="mt-[14px] flex flex-wrap gap-[22px]">
            <Field label="Base" value={profile.location} />
            <Field label="Member" value={profile.member_since} />
            <Field label="Most-driven car" value={totals.topCar} />
            <Field label="Cars raced" value={totals.cars} />
          </div>
        </div>

        {/* gold chip */}
        <div
          className="relative h-[34px] w-[46px] self-start rounded-[7px] shadow-[inset_0_0_0_1px_rgba(0,0,0,0.2)] after:absolute after:inset-[7px] after:rounded-[3px] after:border after:border-black/25 after:content-['']"
          style={{ background: "linear-gradient(135deg, #d9b24a, #f4d97a 45%, #b8902f)" }}
        />
      </div>

      <div className="relative z-[1] mt-[26px] overflow-hidden text-clip whitespace-nowrap border-t border-border pt-4 font-mono text-xs tracking-[0.18em] text-text-dim">
        {mrzLine(data)}
      </div>
    </div>
  );
}
