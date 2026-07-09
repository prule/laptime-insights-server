# Development Workflow — Claude + OpenSpec

This project is built with a **spec-driven** workflow: meaningful changes are first
described as an OpenSpec *change* (why + what + how), then implemented, then archived
into the living specs. Claude Code drives most of this loop with you.

Everything here runs the same whether you're on the host or inside the
[dev container](../.devcontainer/README.md) — the container just guarantees the
toolchain (JDK 21, Node 24.17.0, pnpm 11.8.0) is identical for everyone.

## The tools

- **Claude Code** — the agent that reads the repo, proposes specs, writes code, runs
  tests, and updates docs. Available in the terminal (`claude`) and as the VS Code /
  JetBrains extension (pre-installed in the dev container).
- **OpenSpec** (`openspec/`) — the spec-driven change store:
  - `openspec/specs/` — the *current* agreed behaviour (the source of truth).
  - `openspec/changes/<name>/` — an *in-flight* change: `proposal.md`, `design.md`
    (when cross-cutting), `tasks.md`, and delta `specs/`.
  - `openspec/changes/archive/<date>-<name>/` — completed changes, kept for history.
  - `openspec/config.yaml` — schema + project context Claude uses when writing artifacts.

## The loop

```
 explore ──► propose ──► apply ──► sync/archive
   │            │          │            │
 clarify     write the   implement   fold deltas into
 the idea    change      tasks +      openspec/specs,
             (spec)      tests + docs move change to archive
```

Claude exposes each step as a slash command / skill:

| Step | Command | What it does |
|------|---------|--------------|
| Explore | `/opsx:explore` | Think through an idea before committing to a spec. |
| Propose | `/opsx:propose` | Create a change folder with proposal, design, delta specs, and ordered tasks. |
| Apply | `/opsx:apply` | Implement the tasks — code, tests, sample code, docs. |
| Sync | `/opsx:sync` | Fold a change's delta specs into `openspec/specs/` without archiving. |
| Archive | `/opsx:archive` | Finalize a completed change and move it to `changes/archive/`. |

### 1. Explore (optional)
Fuzzy idea? `/opsx:explore` to clarify requirements and shape the approach before
writing anything durable.

### 2. Propose
`/opsx:propose` describe the change. Claude writes `proposal.md` (WHY before WHAT,
**BREAKING** flagged), delta `specs/`, and a dependency-ordered `tasks.md` — with doc
and test updates as explicit tasks. **Review the proposal before implementing.**

### 3. Apply
`/opsx:apply` works through `tasks.md`, checking off tasks as it goes. Expect it to
touch `app/` and/or `frontend/`, add tests, and update `docs/`. Per project rules it
keeps logic in the backend where reasonable and follows clean/hexagonal architecture.

### 4. Sync / Archive
When the change is implemented and verified, `/opsx:archive` folds the deltas into the
main specs and moves the change into `archive/`. Use `/opsx:sync` if you only want the
specs updated without archiving yet.

## House rules Claude follows

These come from `CLAUDE.md`, `AGENTS.md`, `MEMORY.md`, and `openspec/config.yaml`:

- **Be concise.** Ask for clarification when a requirement is ambiguous.
- **Clean/hexagonal architecture** in `app/`; dependencies point inward.
- **Aggregate on the server** — prefer a dedicated aggregation endpoint over fetching a
  large page and reducing on the client.
- **HATEOAS**: `_links` are capabilities, `enabledFeatures` are UI toggles — never
  conflate them. The frontend follows `_links` rather than hardcoding URLs.
- **Keep docs, tests, and sample code updated** with every change.
- **Log cleanup** in [technical-debt.md](./technical-debt.md); mark items **DONE** when fixed.
- **Format Kotlin before committing:** `./gradlew :app:ktfmtFormat`. The `.githooks`
  pre-commit hook (wired up automatically in the dev container) does this for you.
- **Commit trailer:** `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`.
- **Memory:** when you correct Claude or it learns something, it updates `MEMORY.md`
  in place (replace stale info, don't just append).

## Verifying a change

```bash
./gradlew test                 # backend
cd frontend && pnpm test       # frontend unit (Vitest)
cd frontend && pnpm typecheck  # TS
cd frontend && pnpm e2e        # Playwright e2e (Serenity/JS screenplay)
./run build                    # full build, mirrors CI
```

CI (`.github/workflows/build-and-test.yml`) runs the backend build/test on JDK 21 and
the frontend `pnpm install --frozen-lockfile && pnpm test && pnpm build`, so a green
`./run build` locally is a good predictor of green CI.
