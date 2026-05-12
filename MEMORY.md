# Memory

## Architectural preferences

- **Aggregate on the server.** When a screen needs a count, sum, group-by or bucketed metric, prefer adding a dedicated aggregation endpoint (e.g. `/laps/aggregate?groupBy=track`, `/sessions/aggregate?bucket=week`) over fetching a large `items` page and reducing on the client. Big queries / large responses are not acceptable just to compute a small number. Default to `size: 1` + server `.total` for "how many", and to a purpose-built aggregate response for "how many per X".
- Client-side fallback (e.g. `useMemo` over `items`) is only acceptable as a short-term shim. When you reach for it, add a `docs/technical-debt.md` entry naming the missing endpoint.
