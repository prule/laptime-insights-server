import { mockHandler } from "./mock/handler";
export class ApiError extends Error {
    status;
    path;
    constructor(status, path, message) {
        super(message);
        this.status = status;
        this.path = path;
        this.name = "ApiError";
    }
}
function joinUrl(base, path) {
    if (/^https?:\/\//i.test(path))
        return path;
    if (!base)
        return path;
    if (base.endsWith("/") && path.startsWith("/"))
        return base + path.slice(1);
    if (!base.endsWith("/") && !path.startsWith("/"))
        return `${base}/${path}`;
    return base + path;
}
export async function apiGet(ctx, path) {
    if (ctx.mode === "mock") {
        const result = await mockHandler(path);
        if (result === undefined) {
            throw new ApiError(404, path, `Mock has no handler for ${path}`);
        }
        return result;
    }
    const url = joinUrl(ctx.apiBase, path);
    const response = await fetch(url, { headers: { Accept: "application/json" } });
    if (!response.ok) {
        throw new ApiError(response.status, path, `${response.status} ${response.statusText}`);
    }
    return (await response.json());
}
/** Follow a HATEOAS `_links` value through the same dispatch path. */
export function fetchLink(ctx, links, rel) {
    const target = links[rel];
    if (!target) {
        throw new ApiError(404, rel, `Link relation '${rel}' missing from response`);
    }
    return apiGet(ctx, target);
}
export function buildQuery(params) {
    const entries = Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== "");
    if (entries.length === 0)
        return "";
    const search = new URLSearchParams();
    for (const [k, v] of entries)
        search.set(k, String(v));
    return `?${search.toString()}`;
}
