// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { act, cleanup, render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import {
  LiveEventsProvider,
  useLiveEventStream,
  type WsMessage,
} from "./LiveEventsProvider";
import { LAP_PREFIXES, SESSION_PREFIXES } from "../lib/liveSync";

// Controllable returns for the two provider hooks LiveEventsProvider depends on. `vi.hoisted`
// lets the mock factories below read this object even though they are hoisted above the imports.
const env = vi.hoisted(() => ({
  mode: "mock" as "mock" | "live",
  apiUrl: "http://api.test",
  liveLink: "/api/1/events" as string | undefined,
}));

vi.mock("./DataModeProvider", () => ({
  useDataMode: () => ({ mode: env.mode, apiUrl: env.apiUrl }),
}));

vi.mock("./FeaturesProvider", () => ({
  useFeatures: () => ({ links: { live: env.liveLink } }),
}));

// Minimal WebSocket stand-in that records constructed instances so tests can drive lifecycle
// callbacks (jsdom has no WebSocket implementation).
class MockWebSocket {
  static instances: MockWebSocket[] = [];
  url: string;
  onopen: (() => void) | null = null;
  onclose: (() => void) | null = null;
  onerror: (() => void) | null = null;
  onmessage: ((evt: { data: string }) => void) | null = null;
  close = vi.fn();
  constructor(url: string) {
    this.url = url;
    MockWebSocket.instances.push(this);
  }
}

function StatusProbe() {
  const { status } = useLiveEventStream();
  return <div data-testid="status">{status}</div>;
}

function renderProvider(client: QueryClient) {
  return render(
    <QueryClientProvider client={client}>
      <LiveEventsProvider>
        <StatusProbe />
      </LiveEventsProvider>
    </QueryClientProvider>,
  );
}

beforeEach(() => {
  MockWebSocket.instances = [];
  vi.stubGlobal("WebSocket", MockWebSocket);
  env.mode = "mock";
  env.apiUrl = "http://api.test";
  env.liveLink = "/api/1/events";
});

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe("LiveEventsProvider connection gating", () => {
  it("opens no socket and reports disconnected in MOCK mode", () => {
    env.mode = "mock";
    renderProvider(new QueryClient());

    expect(MockWebSocket.instances).toHaveLength(0);
    expect(screen.getByTestId("status").textContent).toBe("disconnected");
  });

  it("opens no socket when the backend advertises no live rel", () => {
    env.mode = "live";
    env.liveLink = undefined;
    renderProvider(new QueryClient());

    expect(MockWebSocket.instances).toHaveLength(0);
    expect(screen.getByTestId("status").textContent).toBe("disconnected");
  });

  it("connects in LIVE mode and tracks the socket lifecycle", () => {
    env.mode = "live";
    renderProvider(new QueryClient());

    expect(MockWebSocket.instances).toHaveLength(1);
    const ws = MockWebSocket.instances[0]!;
    expect(ws.url).toBe("ws://api.test/api/1/events");
    expect(screen.getByTestId("status").textContent).toBe("connecting");

    act(() => ws.onopen?.());
    expect(screen.getByTestId("status").textContent).toBe("connected");

    act(() => ws.onerror?.());
    expect(screen.getByTestId("status").textContent).toBe("error");
  });

  it("closes the socket on unmount", () => {
    env.mode = "live";
    const { unmount } = renderProvider(new QueryClient());
    const ws = MockWebSocket.instances[0]!;

    unmount();
    expect(ws.close).toHaveBeenCalled();
  });
});

describe("useLiveCacheSync wiring", () => {
  function pushMessage(msg: WsMessage) {
    const ws = MockWebSocket.instances[0]!;
    act(() => ws.onmessage?.({ data: JSON.stringify(msg) }));
  }

  it("invalidates lap query caches on a LapCreated event", () => {
    env.mode = "live";
    const client = new QueryClient();
    const spy = vi.spyOn(client, "invalidateQueries");
    renderProvider(client);

    pushMessage({ type: "LapCreated" } as WsMessage);

    const invalidated = spy.mock.calls.map((c) => (c[0]?.queryKey as string[])?.[0]);
    for (const prefix of LAP_PREFIXES) {
      expect(invalidated).toContain(prefix);
    }
  });

  it("invalidates session query caches on a SessionEnded event", () => {
    env.mode = "live";
    const client = new QueryClient();
    const spy = vi.spyOn(client, "invalidateQueries");
    renderProvider(client);

    pushMessage({ type: "SessionEnded" } as WsMessage);

    const invalidated = spy.mock.calls.map((c) => (c[0]?.queryKey as string[])?.[0]);
    for (const prefix of SESSION_PREFIXES) {
      expect(invalidated).toContain(prefix);
    }
  });

  it("does not invalidate caches for high-frequency PlayerCarUpdated frames", () => {
    env.mode = "live";
    const client = new QueryClient();
    const spy = vi.spyOn(client, "invalidateQueries");
    renderProvider(client);

    pushMessage({ type: "PlayerCarUpdated" } as WsMessage);

    expect(spy).not.toHaveBeenCalled();
  });
});
