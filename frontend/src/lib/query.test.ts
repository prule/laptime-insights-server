import { describe, expect, it } from "vitest";
import { combineQueryState } from "./query";

type Q = { isLoading: boolean; isError: boolean; error: Error | null };

const ok: Q = { isLoading: false, isError: false, error: null };
const loading: Q = { isLoading: true, isError: false, error: null };
const failing = (e: Error): Q => ({ isLoading: false, isError: true, error: e });

describe("combineQueryState", () => {
  it("reports not loading and no error when every query is ok", () => {
    expect(combineQueryState([ok, ok])).toEqual({ isLoading: false, error: null });
  });

  it("reports loading if any query is loading", () => {
    expect(combineQueryState([ok, loading, ok]).isLoading).toBe(true);
  });

  it("returns the first error in supplied order", () => {
    const first = new Error("first");
    const second = new Error("second");
    expect(combineQueryState([ok, failing(first), failing(second)]).error).toBe(first);
  });

  it("returns null when no queries are in an error state", () => {
    expect(combineQueryState([loading, ok]).error).toBeNull();
  });
});
