/**
 * Single app-level WebSocket to the index `live` rel.
 *
 * Before this provider existed, only `LiveScreen` opened the `/api/1/events` socket, so a
 * completed lap never reached the other (TanStack Query backed) screens until a manual
 * refresh. This provider opens ONE connection, fans every frame out to subscribers, and
 * mounts {@link useLiveCacheSync} so domain events invalidate the relevant query caches
 * app-wide. `LiveScreen` subscribes here too rather than opening a second socket.
 *
 * The connection is inert unless LIVE data mode is active and the backend advertises a
 * `live` rel — MOCK mode and feature-off backends open no socket.
 */
import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { useQueryClient } from "@tanstack/react-query";
import type { LapResource, Links, SessionResource } from "../api/types";
import { invalidationPrefixesFor, shouldConnect } from "../lib/liveSync";
import { useDataMode } from "./DataModeProvider";
import { useFeatures } from "./FeaturesProvider";

// ─── WebSocket message types (mirror WebSocketMessage.kt) ────────────────────

export interface PlayerCarUpdateData {
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

export type WsMessage =
  | { type: "ServerStarted" }
  | { type: "SessionCreated" | "SessionStarted" | "SessionUpdated"; data: SessionResource }
  | { type: "SessionEnded"; data: SessionResource }
  | { type: "LapCreated"; data: LapResource }
  | { type: "PlayerCarUpdated"; data: PlayerCarUpdateData };

export type ConnectionStatus = "connecting" | "connected" | "disconnected" | "error";

type WsListener = (msg: WsMessage) => void;

interface LiveEventsContextValue {
  status: ConnectionStatus;
  /** Register a frame listener; returns an unsubscribe fn. No-op when no socket is open. */
  subscribe: (listener: WsListener) => () => void;
}

const LiveEventsContext = createContext<LiveEventsContextValue | null>(null);

/** Derive the ws URL for the `live` rel from the api base (or current origin). */
function liveSocketUrl(apiUrl: string, liveLink: string): string {
  const base = apiUrl || window.location.origin;
  return base.replace(/^http/, "ws") + liveLink;
}

export function LiveEventsProvider({ children }: { children: ReactNode }) {
  const { mode, apiUrl } = useDataMode();
  const { links }: { links: Links } = useFeatures();
  const liveLink = links.live;
  const enabled = shouldConnect(mode, liveLink);

  const [status, setStatus] = useState<ConnectionStatus>("disconnected");
  const listenersRef = useRef<Set<WsListener>>(new Set());

  const subscribe = useCallback((listener: WsListener) => {
    listenersRef.current.add(listener);
    return () => {
      listenersRef.current.delete(listener);
    };
  }, []);

  useEffect(() => {
    if (!enabled || !liveLink) {
      setStatus("disconnected");
      return;
    }
    const wsUrl = liveSocketUrl(apiUrl, liveLink);
    let ws: WebSocket;
    let closed = false;

    function connect() {
      setStatus("connecting");
      ws = new WebSocket(wsUrl);
      ws.onopen = () => setStatus("connected");
      ws.onerror = () => setStatus("error");
      ws.onclose = () => {
        if (!closed) {
          setStatus("disconnected");
          setTimeout(connect, 3000); // reconnect
        }
      };
      ws.onmessage = (evt: MessageEvent) => {
        let msg: WsMessage;
        try {
          msg = JSON.parse(evt.data as string) as WsMessage;
        } catch {
          return;
        }
        listenersRef.current.forEach((l) => l(msg));
      };
    }

    connect();
    return () => {
      closed = true;
      ws?.close();
    };
  }, [enabled, apiUrl, liveLink]);

  const value = useMemo<LiveEventsContextValue>(
    () => ({ status, subscribe }),
    [status, subscribe],
  );

  return (
    <LiveEventsContext.Provider value={value}>
      <LiveCacheSync />
      {children}
    </LiveEventsContext.Provider>
  );
}

export function useLiveEventStream(): LiveEventsContextValue {
  const ctx = useContext(LiveEventsContext);
  if (!ctx) throw new Error("useLiveEventStream must be used inside LiveEventsProvider");
  return ctx;
}

// ─── Cache sync ─────────────────────────────────────────────────────────────

/** Subscribes to the shared stream and invalidates query caches on domain events. */
export function useLiveCacheSync() {
  const queryClient = useQueryClient();
  const { subscribe } = useLiveEventStream();

  useEffect(() => {
    return subscribe((msg) => {
      for (const prefix of invalidationPrefixesFor(msg.type)) {
        queryClient.invalidateQueries({ queryKey: [prefix] });
      }
    });
  }, [subscribe, queryClient]);
}

/** Renderless child that runs the cache-sync effect inside the provider. */
function LiveCacheSync() {
  useLiveCacheSync();
  return null;
}
