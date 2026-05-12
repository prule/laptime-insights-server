import { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { appendQuery } from "../api/client";
import { Card } from "../components/ui/Card";
import { LapTable } from "../components/LapTable";
import { SectionHeader } from "../components/ui/SectionHeader";
import { formatLapTime } from "../lib/format";
import { useDataMode } from "../providers/DataModeProvider";
import { useFeatures } from "../providers/FeaturesProvider";
import type { LapResource, Links, Page, SessionResource } from "../api/types";

// ─── WebSocket message types (mirror WebSocketMessage.kt) ────────────────────

interface PlayerCarUpdateData {
  sessionUid: string;
  gear: number;
  kmh: number;
  splinePosition: number;
  worldPosX: number;
  worldPosY: number;
  racePosition: number;
  currentLapTimeMs: number;
  currentLapIsInvalid: boolean;
  delta: number;
  bestLapTimeMs: number;
  lastLapTimeMs: number;
}

type WsMessage =
  | { type: "ServerStarted" }
  | { type: "SessionCreated" | "SessionStarted" | "SessionUpdated"; data: SessionResource }
  | { type: "LapCreated"; data: LapResource }
  | { type: "PlayerCarUpdated"; data: PlayerCarUpdateData };

// ─── Hook ─────────────────────────────────────────────────────────────────────

type ConnectionStatus = "connecting" | "connected" | "disconnected" | "error";

function useLiveEvents(apiUrl: string, indexLinks: Links) {
  const [status, setStatus] = useState<ConnectionStatus>("connecting");
  const [session, setSession] = useState<SessionResource | null>(null);
  const [telemetry, setTelemetry] = useState<PlayerCarUpdateData | null>(null);
  const [laps, setLaps] = useState<LapResource[]>([]);
  // Accumulated world-space coords to draw the track outline.
  const trackPointsRef = useRef<{ x: number; y: number }[]>([]);
  const [trackPoints, setTrackPoints] = useState<{ x: number; y: number }[]>([]);
  const lastSessionUidRef = useRef<string | null>(null);
  // Tracks the player's car id without needing to re-create the WS handler on every session update.
  const playerCarIdRef = useRef<number | null>(null);
  // The full session resource — we keep a ref so `LapCreated` handlers (which run inside a
  // stale closure) can still follow `session._links.laps`.
  const sessionRef = useRef<SessionResource | null>(null);
  useEffect(() => {
    sessionRef.current = session;
  }, [session]);

  /**
   * Fetch session details via REST when we connect mid-session (no lifecycle WS event fires).
   * Composes the URL from the index `sessions` rel; when that rel is absent the sessions
   * feature is off and we skip the call.
   */
  function fetchSession(uid: string, base: string) {
    if (!indexLinks.sessions) return;
    const apiBase = base.replace(/^ws/, "http");
    fetch(`${apiBase}${indexLinks.sessions}/${uid}`)
      .then((r) => (r.ok ? r.json() : null))
      .then((data: SessionResource | null) => {
        if (data) {
          setSession(data);
          playerCarIdRef.current = data.playerCarId;
          // Once we have the session resource, follow its `laps` rel rather than rebuilding
          // the URL ourselves.
          fetchSessionLaps(data, base);
        }
      })
      .catch(() => {/* ignore — we'll show what we can */});
  }

  /**
   * Fetch the full list of laps for `session` (restricted to its `playerCarId` when known)
   * and replace local lap state. Follows `session._links.laps` so the URL — and the
   * laps-feature gate — stays server-driven.
   */
  function fetchSessionLaps(session: SessionResource, base: string) {
    const lapsLink = session._links.laps;
    if (!lapsLink) return;
    const apiBase = base.replace(/^ws/, "http");
    const url =
      apiBase +
      appendQuery(lapsLink, {
        page: 1,
        size: 200,
        sort: "lapNumber:DESC",
        carId: session.playerCarId ?? undefined,
      });
    fetch(url)
      .then((r) => (r.ok ? r.json() : null))
      .then((data: Page<LapResource> | null) => {
        if (!data) return;
        setLaps(data.items);
      })
      .catch(() => {/* ignore — we still have the optimistic WS-driven state */});
  }

  useEffect(() => {
    const liveLink = indexLinks.live;
    const sessionsLink = indexLinks.sessions;
    if (!liveLink) return;
    const base = apiUrl || window.location.origin;
    const wsUrl = base.replace(/^http/, "ws") + liveLink;
    let ws: WebSocket;
    let closed = false;

    // On mount, look up the most recent session and load its current state and lap list so the
    // page is populated even before any WS event fires. Without this, a freshly loaded page
    // during a lull in telemetry would stay empty until the next PlayerCarUpdated/LapCreated.
    // (Sessions no longer carry a finished flag — the most recent one is treated as live.)
    if (sessionsLink) {
      const apiBase = base.replace(/^ws/, "http");
      const url =
        apiBase + appendQuery(sessionsLink, { sort: "startedAt:DESC", size: 1 });
      fetch(url)
        .then((r) => (r.ok ? r.json() : null))
        .then((page: Page<SessionResource> | null) => {
          const latest = page?.items?.[0];
          if (!latest) return;
          setSession(latest);
          playerCarIdRef.current = latest.playerCarId;
          lastSessionUidRef.current = latest.uid;
          fetchSessionLaps(latest, base);
        })
        .catch(() => {/* ignore — WS will populate state once events arrive */});
    }

    function connect() {
      setStatus("connecting");
      ws = new WebSocket(wsUrl);

      ws.onopen = () => setStatus("connected");
      ws.onerror = () => setStatus("error");
      ws.onclose = () => {
        if (!closed) {
          setStatus("disconnected");
          // Reconnect after 3 s.
          setTimeout(connect, 3000);
        }
      };

      ws.onmessage = (evt: MessageEvent) => {
        let msg: WsMessage;
        try {
          msg = JSON.parse(evt.data as string) as WsMessage;
        } catch {
          return;
        }

        switch (msg.type) {
          case "ServerStarted":
            // Server is fresh — clear all accumulated live state.
            setSession(null);
            setTelemetry(null);
            setLaps([]);
            lastSessionUidRef.current = null;
            playerCarIdRef.current = null;
            trackPointsRef.current = [];
            setTrackPoints([]);
            break;
          case "SessionCreated":
          case "SessionStarted":
          case "SessionUpdated": {
            const s = msg.data as SessionResource;
            setSession(s);
            playerCarIdRef.current = s.playerCarId;
            // Reset laps and track shape when a new session begins.
            if (lastSessionUidRef.current !== s.uid) {
              lastSessionUidRef.current = s.uid;
              setLaps([]);
              trackPointsRef.current = [];
              setTrackPoints([]);
              setTelemetry(null);
            }
            break;
          }
          case "LapCreated": {
            const lap = msg.data as LapResource;
            // Ignore laps from other cars or other sessions — the live page only tracks the player.
            if (lap.sessionUid !== lastSessionUidRef.current) break;
            if (playerCarIdRef.current !== null && lap.carId !== playerCarIdRef.current) break;
            // Optimistically prepend so the UI reflects the new lap immediately,
            // even if the follow-up REST fetch is slow.
            setLaps((prev) => (prev.some((l) => l.uid === lap.uid) ? prev : [lap, ...prev]));
            // Then refetch the authoritative full list. We need the current session resource
            // (for its `_links.laps`) to do that — `sessionRef.current` carries it.
            const currentSession = sessionRef.current;
            if (currentSession) fetchSessionLaps(currentSession, base);
            break;
          }
          case "PlayerCarUpdated": {
            const t = msg.data as PlayerCarUpdateData;
            setTelemetry(t);
            // If we connected mid-session we won't have received a SessionCreated/Started event.
            // Fetch the session once via REST so the header shows track + session type.
            if (lastSessionUidRef.current !== t.sessionUid) {
              lastSessionUidRef.current = t.sessionUid;
              fetchSession(t.sessionUid, base);
            }
            // Accumulate track points for the outline (cap at 2000 to avoid memory growth).
            // Sync state every 5 points so the outline grows quickly on first lap.
            const pts = trackPointsRef.current;
            if (pts.length < 2000) {
              pts.push({ x: t.worldPosX, y: t.worldPosY });
              if (pts.length % 5 === 0) setTrackPoints([...pts]);
            }
            break;
          }
        }
      };
    }

    connect();
    return () => {
      closed = true;
      ws?.close();
    };
  }, [apiUrl, indexLinks.live, indexLinks.sessions]);

  return { status, session, telemetry, laps, trackPoints };
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

// Backend uses Long.MAX_VALUE (9_223_372_036_854_775_807) for "no lap yet".
// JS can't represent that exactly, but any value > Number.MAX_SAFE_INTEGER signals "unset".
const SENTINEL = Number.MAX_SAFE_INTEGER;

function formatDelta(ms: number): string {
  if (ms === 0) return "±0.000";
  const sign = ms > 0 ? "+" : "-";
  const abs = Math.abs(ms);
  return `${sign}${(abs / 1000).toFixed(3)}`;
}

function formatCurrentLap(ms: number): string {
  if (ms <= 0) return "0:00.000";
  return formatLapTime(ms);
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function ConnectionBadge({ status }: { status: ConnectionStatus }) {
  const cfg = {
    connecting: { dot: "bg-warn animate-pulse", label: "CONNECTING" },
    connected: { dot: "bg-ok shadow-[0_0_6px_#22c55e]", label: "CONNECTED" },
    disconnected: { dot: "bg-text-muted", label: "DISCONNECTED" },
    error: { dot: "bg-accent", label: "ERROR" },
  }[status];
  return (
    <div className="flex items-center gap-2">
      <span className={`h-2 w-2 flex-shrink-0 rounded-full ${cfg.dot}`} />
      <span className="font-mono text-[10px] tracking-[0.08em] text-text-muted">{cfg.label}</span>
    </div>
  );
}

function GearDisplay({ gear, invalid }: { gear: number; invalid: boolean }) {
  return (
    <div className={`flex flex-col items-center justify-center rounded-xl border p-6 ${invalid ? "border-accent/40 bg-accent/5" : "border-border bg-surface-active"}`}>
      <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted mb-1">Gear</div>
      <div className={`font-mono text-7xl font-bold leading-none ${invalid ? "text-accent" : "text-text"}`}>
        {gear === 0 ? "R" : gear === 1 ? "N" : gear - 1}
      </div>
      {invalid && (
        <div className="mt-2 font-mono text-[10px] uppercase tracking-widest text-accent">Invalid lap</div>
      )}
    </div>
  );
}

function SpeedDisplay({ kmh }: { kmh: number }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-border bg-surface-active p-6">
      <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted mb-1">Speed</div>
      <div className="font-mono text-5xl font-bold text-cyan leading-none">{kmh}</div>
      <div className="font-mono text-[10px] text-text-muted mt-1">km/h</div>
    </div>
  );
}

function PositionDisplay({ position }: { position: number }) {
  return (
    <div className="flex flex-col items-center justify-center rounded-xl border border-border bg-surface-active p-6">
      <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted mb-1">Position</div>
      <div className="font-mono text-5xl font-bold text-accent leading-none">P{position}</div>
    </div>
  );
}

/**
 * Simple track map that draws accumulated worldPosX/Y samples and a moving dot.
 *
 * Bounds are computed from `points` only (not the current position) so the
 * outline stays stable as the car moves — including the current position in
 * bounds would rescale everything on every telemetry update.
 */
function LiveTrackMap({
  points,
  currentX,
  currentY,
}: {
  points: { x: number; y: number }[];
  currentX: number;
  currentY: number;
}) {
  // Recompute outline only when the accumulated points array changes (every 20 frames).
  const { outline, bounds } = useMemo(() => {
    if (points.length < 2) return { outline: "", bounds: null };

    const xs = points.map((p) => p.x);
    const ys = points.map((p) => p.y);
    const minX = Math.min(...xs);
    const maxX = Math.max(...xs);
    const minY = Math.min(...ys);
    const maxY = Math.max(...ys);
    const rangeX = maxX - minX || 1;
    const rangeY = maxY - minY || 1;

    const toSvg = (x: number, y: number) => ({
      nx: ((x - minX) / rangeX) * 960 + 20,
      ny: ((y - minY) / rangeY) * 960 + 20,
    });

    const path =
      points
        .map((p, i) => {
          const { nx, ny } = toSvg(p.x, p.y);
          return `${i === 0 ? "M" : "L"}${nx.toFixed(1)},${ny.toFixed(1)}`;
        })
        .join(" ");

    return { outline: path, bounds: { minX, maxX, minY, maxY, rangeX, rangeY } };
  }, [points]);

  // Dot position recalculates cheaply on every telemetry update without touching the outline.
  const { cx, cy } = useMemo(() => {
    if (!bounds) return { cx: 500, cy: 500 };
    return {
      cx: ((currentX - bounds.minX) / bounds.rangeX) * 960 + 20,
      cy: ((currentY - bounds.minY) / bounds.rangeY) * 960 + 20,
    };
  }, [bounds, currentX, currentY]);

  if (points.length < 5) {
    return (
      <div className="flex h-full items-center justify-center font-mono text-[11px] text-text-muted">
        Accumulating track data…
      </div>
    );
  }

  return (
    <svg viewBox="0 0 1000 1000" className="h-full w-full">
      {outline && (
        <path
          d={outline}
          fill="none"
          stroke="#ffffff12"
          strokeWidth="8"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      )}
      {/* Pulse ring */}
      <circle cx={cx} cy={cy} r={28} fill="none" stroke="#00d4ff" strokeWidth="2" opacity={0.35} />
      {/* Position dot */}
      <circle cx={cx} cy={cy} r={16} fill="#00d4ff" opacity={0.95} />
    </svg>
  );
}

// ─── Screen ───────────────────────────────────────────────────────────────────

export function LiveScreen() {
  const navigate = useNavigate();
  const { mode, apiUrl } = useDataMode();
  const { links } = useFeatures();
  const { status, session, telemetry, laps, trackPoints } = useLiveEvents(apiUrl, links);

  const isLiveMode = mode === "live";

  // Only show laps for this session driven by the player's car.
  const playerCarId = session?.playerCarId ?? null;
  const sessionUid = session?.uid ?? null;
  const playerLaps = laps.filter(
    (l) =>
      (sessionUid === null || l.sessionUid === sessionUid) &&
      (playerCarId === null || l.carId === playerCarId),
  );

  if (!isLiveMode) {
    return (
      <div className="flex h-full items-center justify-center p-8">
        <div className="max-w-sm text-center">
          <div className="mb-3 font-mono text-3xl text-text-muted">◉</div>
          <div className="font-sans text-base font-semibold text-text mb-2">Switch to Live mode</div>
          <div className="font-sans text-sm text-text-muted">
            The Live page requires a connection to the backend. Use the toggle in the sidebar to switch from mock to live data.
          </div>
        </div>
      </div>
    );
  }

  const t = telemetry;

  return (
    <div className="h-full overflow-y-auto px-8 py-7">
      {/* Header */}
      <div className="mb-6 flex items-center gap-4 border-b border-border pb-5">
        <div className="flex h-[52px] w-[52px] items-center justify-center rounded-[10px] border border-accent/[0.3] bg-gradient-to-br from-accent/20 to-accent/10 font-mono text-lg font-bold text-accent">
          ◉
        </div>
        <div>
          <div className="font-sans text-xl font-semibold text-text">Live</div>
          <div className="font-sans text-[13px] text-text-muted">
            {session
              ? [
                  session.track ?? "Unknown track",
                  session.sessionType,
                  session.car ?? "Unknown car",
                  session.playerCarId !== null ? `#${session.playerCarId}` : null,
                ]
                  .filter(Boolean)
                  .join(" · ")
              : "Waiting for session…"}
          </div>
        </div>
        <div className="ml-auto flex items-center gap-4">
          <ConnectionBadge status={status} />
        </div>
      </div>

      {!t && (
        <div className="mb-5 flex items-center justify-center rounded-md border border-border bg-surface-active p-8">
          <span className="font-mono text-[12px] text-text-muted">
            {status === "connected" ? "Waiting for telemetry…" : "Connect the server to ACC to see live data."}
          </span>
        </div>
      )}

      {t && (
        <>
          {/* HUD row */}
          <div className="mb-5 grid grid-cols-3 gap-4">
            <GearDisplay gear={t.gear} invalid={t.currentLapIsInvalid} />
            <SpeedDisplay kmh={t.kmh} />
            <PositionDisplay position={t.racePosition} />
          </div>

          {/* Lap times row */}
          <div className="mb-5 grid grid-cols-3 gap-4">
            <Card>
              <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted mb-1">Current Lap</div>
              <div className={`font-mono text-2xl font-bold ${t.currentLapIsInvalid ? "text-accent" : "text-text"}`}>
                {formatCurrentLap(t.currentLapTimeMs)}
              </div>
            </Card>
            <Card>
              <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted mb-1">Best Lap</div>
              <div className="font-mono text-2xl font-bold text-ok">
                {t.bestLapTimeMs >= SENTINEL ? "—" : formatLapTime(t.bestLapTimeMs)}
              </div>
            </Card>
            <Card>
              <div className="font-mono text-[10px] uppercase tracking-[0.08em] text-text-muted mb-1">Delta</div>
              <div className={`font-mono text-2xl font-bold ${t.delta > 0 ? "text-accent" : t.delta < 0 ? "text-ok" : "text-text-muted"}`}>
                {t.bestLapTimeMs >= SENTINEL ? "—" : formatDelta(t.delta)}
              </div>
            </Card>
          </div>

          {/* Track map */}
          <Card className="mb-5">
            <SectionHeader
              title="Track position"
              sub={`Spline: ${(t.splinePosition * 100).toFixed(1)}%`}
            />
            <div className="h-64">
              <LiveTrackMap
                points={trackPoints}
                currentX={t.worldPosX}
                currentY={t.worldPosY}
              />
            </div>
          </Card>
        </>
      )}

      {/* Completed laps table */}
      <Card>
        <SectionHeader title="Completed laps" sub={playerLaps.length > 0 ? `${playerLaps.length} lap${playerLaps.length !== 1 ? "s" : ""}` : undefined} />
        {playerLaps.length === 0 ? (
          <div className="py-6 text-center font-mono text-[11px] text-text-muted">No laps completed yet</div>
        ) : (
          <LapTable
            laps={playerLaps}
            onSessionClick={(uid) => navigate(`/sessions/${uid}`)}
          />
        )}
      </Card>
    </div>
  );
}
