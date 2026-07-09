# Dev Container

A reproducible development environment for laptime-insights. It bundles everything
both halves of the stack need, so you never install JDK / Node / pnpm on the host:

| Tool | Version | Why |
|------|---------|-----|
| JDK (Temurin) | 21 | Kotlin/Ktor/Exposed backend (`app/`) |
| Node | 24.17.0 | React/Vite frontend (`frontend/`, `landing/`) — matches `.node-version` |
| pnpm | 11.8.0 | Package manager (pinned via `package.json` `packageManager`) |
| Gradle | 9.6.0 | Via the checked-in `./gradlew` wrapper (nothing to install) |
| Playwright + Chromium | latest | Frontend e2e (`pnpm e2e`) |

The container has a **static name: `laptime-insights-dev`**. `docker ps`, `docker exec`,
and IDE reconnects always target the same container.

## What's inside

- **Git hooks** wired to `.githooks` (ktfmt formats Kotlin on commit).
- **Dependencies pre-installed** — frontend + landing `pnpm install`, backend Gradle
  classes warmed, Playwright browsers downloaded. See `post-create.sh`.
- **Ports auto-forwarded**: `8000` backend API + Swagger UI, `5173` frontend dev
  server, `9092` optional H2 TCP.
- **Dev DB defaults** (`remoteEnv` in `devcontainer.json`): in-memory H2 seeded with
  sample data (`DB_SEED=true`) and all feature flags on, so `./run serve` works on
  first boot. Override in your shell for other scenarios (e.g. a file-backed DB).
- **Caches persisted** in Docker volumes (`~/.gradle`, pnpm store) so rebuilds stay fast.

## Opening the container

### VS Code
1. Install the **Dev Containers** extension (`ms-vscode-remote.remote-containers`).
2. Open the repo, then **Reopen in Container** (Command Palette → *Dev Containers:
   Reopen in Container*).
3. First build runs `post-create.sh` (a few minutes). When it finishes, open a
   terminal and run `./run serve`.

### IntelliJ IDEA / WebStorm
JetBrains reads the same `devcontainer.json`.
1. **New / existing project** → *Remote Development* → **Dev Containers** → *New Dev
   Container* → **From devcontainer.json** (point it at `.devcontainer/devcontainer.json`).
   You can also right-click `devcontainer.json` in the editor and choose *Create Dev
   Container and Mount Sources*.
2. The IDE builds the container, installs its backend inside it, and connects.
3. IDEA gets the Kotlin/Gradle project; WebStorm is best pointed at `frontend/`.

> Both IDEs reuse the container named `laptime-insights-dev`; closing and reopening
> reconnects rather than rebuilding.

## Everyday commands (run inside the container)

```bash
./run serve      # start backend (:8000) + frontend (:5173) together
./run build      # build frontend then backend
./run setup      # re-run hooks + dependency install (post-create does this for you)

./gradlew :app:run    # backend only
./gradlew test        # backend tests
cd frontend && pnpm dev        # frontend only (mock data by default)
cd frontend && pnpm test       # unit tests (Vitest)
cd frontend && pnpm e2e        # Playwright e2e
```

- Frontend: <http://localhost:5173> (sidebar toggles **mock** ↔ **live** data)
- Swagger UI: <http://localhost:8000/swaggerUI> · OpenAPI: <http://localhost:8000/openapi>

## Development process (Claude + OpenSpec)

The container ships the **Claude Code** VS Code extension and the `openspec/`
spec-driven workflow. See **[docs/development-workflow.md](../docs/development-workflow.md)**
for the full loop (propose → apply → archive) and how Claude fits into it.

## Rebuilding / troubleshooting

- **Rebuild from scratch:** VS Code → *Dev Containers: Rebuild Container*. To also
  drop the caches: `docker volume rm laptime-insights-gradle laptime-insights-pnpm`.
- **Playwright deps failed during create:** re-run
  `cd frontend && pnpm exec playwright install --with-deps chromium`.
- **Port already in use:** something else holds `8000`/`5173` on the host — stop it or
  edit `forwardPorts` in `devcontainer.json`.
