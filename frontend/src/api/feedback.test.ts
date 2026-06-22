import { afterEach, describe, expect, it, vi } from "vitest";
import type { FeedbackConfig } from "../config/feedback";
import { submitFeedback, validateFeedback, type FeedbackPayload } from "./feedback";

const config: FeedbackConfig = {
  formUrl: "https://docs.google.com/forms/d/e/abc/formResponse",
  entryIds: {
    type: "entry.1",
    message: "entry.2",
    email: "entry.3",
    version: "entry.4",
    screen: "entry.5",
  },
};

const payload: FeedbackPayload = {
  type: "bug",
  message: "  it crashed  ",
  email: "me@example.com",
  appVersion: "1.2.3",
  screen: "/laps",
};

afterEach(() => {
  vi.restoreAllMocks();
});

describe("validateFeedback", () => {
  it("rejects an empty / whitespace-only message", () => {
    expect(validateFeedback("", "").message).toBeDefined();
    expect(validateFeedback("   ", "").message).toBeDefined();
  });

  it("rejects a syntactically invalid email", () => {
    expect(validateFeedback("hi", "foo@").email).toBeDefined();
  });

  it("accepts a message with no email or a valid email", () => {
    expect(validateFeedback("hi", "")).toEqual({});
    expect(validateFeedback("hi", "me@example.com")).toEqual({});
  });
});

describe("submitFeedback", () => {
  it("maps each field to its configured entry id and posts no-cors in live mode", async () => {
    const fetchMock = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("fetch", fetchMock);

    await submitFeedback(payload, config, "live");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0]!;
    expect(url).toBe(config.formUrl);
    expect(init.method).toBe("POST");
    expect(init.mode).toBe("no-cors");

    const body = init.body as FormData;
    expect(body.get("entry.1")).toBe("Bug");
    expect(body.get("entry.2")).toBe("it crashed"); // trimmed
    expect(body.get("entry.3")).toBe("me@example.com");
    expect(body.get("entry.4")).toBe("1.2.3");
    expect(body.get("entry.5")).toBe("/laps");
  });

  it("omits fields whose entry id is not configured", async () => {
    const fetchMock = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("fetch", fetchMock);

    const minimal: FeedbackConfig = { formUrl: config.formUrl, entryIds: { type: "entry.1", message: "entry.2" } };
    await submitFeedback(payload, minimal, "live");

    const body = fetchMock.mock.calls[0]![1].body as FormData;
    expect(body.get("entry.1")).toBe("Bug");
    expect(body.get("entry.2")).toBe("it crashed");
    expect(body.has("entry.3")).toBe(false);
    expect(body.has("entry.4")).toBe(false);
  });

  it("treats a resolved fetch as success (opaque no-cors response)", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(undefined));
    await expect(submitFeedback(payload, config, "live")).resolves.toBeUndefined();
  });

  it("does not call the network in mock mode", async () => {
    const fetchMock = vi.fn();
    vi.stubGlobal("fetch", fetchMock);

    await submitFeedback(payload, config, "mock");

    expect(fetchMock).not.toHaveBeenCalled();
  });
});
