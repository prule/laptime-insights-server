/**
 * Thin fetch wrapper supporting both **live** and **mock** data modes.
 *
 * In live mode it issues a real HTTP request to the backend (relative URLs are
 * routed via the Vite dev proxy, absolute URLs hit the configured host).
 *
 * In mock mode it routes the request to an in-memory handler that mirrors the
 * Ktor backend's responses — including HATEOAS `_links` — so the rest of the
 * UI stays unchanged when toggling modes.
 *
 * All endpoints follow the convention: append the `path` to the configured
 * `apiBase` (or use `path` directly when it starts with `http://` /
 * `https://`). HATEOAS link traversal can use `fetchLink` which forwards the
 * link target through the same dispatch path.
 */
import type { DataMode } from "../providers/DataModeProvider";
import { mockHandler } from "./mock/handler";

export interface ApiContext {
  mode: DataMode;
  apiBase: string;
}

export class ApiError extends Error {
  constructor(
    public status: number,
    public path: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

function joinUrl(base: string, path: string): string {
  if (/^https?:\/\//i.test(path)) return path;
  if (!base) return path;
  if (base.endsWith("/") && path.startsWith("/")) return base + path.slice(1);
  if (!base.endsWith("/") && !path.startsWith("/")) return `${base}/${path}`;
  return base + path;
}

export async function apiGet<T>(ctx: ApiContext, path: string): Promise<T> {
  if (ctx.mode === "mock") {
    const result = await mockHandler(path);
    if (result === undefined) {
      throw new ApiError(404, path, `Mock has no handler for ${path}`);
    }
    return result as T;
  }
  const url = joinUrl(ctx.apiBase, path);
  const response = await fetch(url, { headers: { Accept: "application/json" } });
  if (!response.ok) {
    throw new ApiError(response.status, path, `${response.status} ${response.statusText}`);
  }
  return (await response.json()) as T;
}

/** Follow a HATEOAS `_links` value through the same dispatch path. */
export function fetchLink<T>(ctx: ApiContext, links: Record<string, string>, rel: string) {
  const target = links[rel];
  if (!target) {
    throw new ApiError(404, rel, `Link relation '${rel}' missing from response`);
  }
  return apiGet<T>(ctx, target);
}

export function buildQuery(params: Record<string, string | number | boolean | undefined | null>): string {
  const entries = Object.entries(params).filter(
    ([, v]) => v !== undefined && v !== null && v !== "",
  );
  if (entries.length === 0) return "";
  const search = new URLSearchParams();
  for (const [k, v] of entries) search.set(k, String(v));
  return `?${search.toString()}`;
}
