// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import type { FeedbackConfig } from "../../config/feedback";
import { DataModeProvider } from "../../providers/DataModeProvider";
import { FeedbackButton } from "./FeedbackButton";

const cfg = vi.hoisted(() => ({ value: null as FeedbackConfig | null }));
const submitMock = vi.hoisted(() => vi.fn());

vi.mock("../../config/feedback", () => ({
  readFeedbackConfig: () => cfg.value,
}));

vi.mock("../../api/feedback", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../api/feedback")>();
  return { ...actual, submitFeedback: submitMock };
});

function renderAt(path = "/laps") {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <DataModeProvider>
        <FeedbackButton />
      </DataModeProvider>
    </MemoryRouter>,
  );
}

const CONFIGURED: FeedbackConfig = {
  formUrl: "https://example.com/formResponse",
  entryIds: { type: "entry.1", message: "entry.2" },
};

const open = () => fireEvent.click(screen.getByRole("button", { name: "Feedback" }));
const send = () => fireEvent.click(screen.getByRole("button", { name: "Send feedback" }));
const setMessage = (v: string) =>
  fireEvent.change(screen.getByPlaceholderText(/Describe the bug/), { target: { value: v } });
const setEmail = (v: string) =>
  fireEvent.change(screen.getByPlaceholderText("you@example.com"), { target: { value: v } });

beforeEach(() => {
  cfg.value = CONFIGURED;
  submitMock.mockReset().mockResolvedValue(undefined);
});

afterEach(cleanup);

describe("launcher visibility", () => {
  it("is hidden when feedback is not configured", () => {
    cfg.value = null;
    renderAt();
    expect(screen.queryByRole("button", { name: "Feedback" })).toBeNull();
  });

  it("is visible when feedback is configured", () => {
    renderAt();
    expect(screen.getByRole("button", { name: "Feedback" })).toBeTruthy();
  });
});

describe("feedback form", () => {
  it("blocks submission with an empty message", () => {
    renderAt();
    open();
    send();
    expect(screen.getByText("Please enter a message.")).toBeTruthy();
    expect(submitMock).not.toHaveBeenCalled();
  });

  it("blocks submission with an invalid email", () => {
    renderAt();
    open();
    setMessage("something broke");
    setEmail("foo@");
    send();
    expect(screen.getByText("Please enter a valid email address.")).toBeTruthy();
    expect(submitMock).not.toHaveBeenCalled();
  });

  it("submits with auto-attached context and shows success", async () => {
    renderAt("/compare");
    open();
    setMessage("great app");
    send();

    await waitFor(() => expect(submitMock).toHaveBeenCalledTimes(1));
    const [payload] = submitMock.mock.calls[0]!;
    expect(payload).toMatchObject({ type: "bug", message: "great app", screen: "/compare" });
    expect(payload.appVersion).toBeDefined();

    expect(await screen.findByText(/your feedback was sent/i)).toBeTruthy();
  });

  it("shows a retryable error when submission fails", async () => {
    submitMock.mockRejectedValueOnce(new Error("network"));
    renderAt();
    open();
    setMessage("keeps the draft");
    send();

    expect(await screen.findByText(/Couldn’t send your feedback/i)).toBeTruthy();
    // Message is preserved so the user can retry.
    expect(screen.getByDisplayValue("keeps the draft")).toBeTruthy();
  });
});
