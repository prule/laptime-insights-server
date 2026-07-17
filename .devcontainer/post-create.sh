#!/usr/bin/env bash
#
# One-time provisioning for the laptime-insights dev container.
# Runs automatically after the container is created (postCreateCommand).
# Safe to re-run by hand:  bash .devcontainer/post-create.sh
#
set -euo pipefail

# Named-volume mounts (~/.gradle, PNPM_HOME, Playwright browsers, shared pnpm
# store) are created root-owned on first boot. Hand them to the container user so
# installs don't hit permission errors. This must stay ahead of corepack below,
# which writes to ~/.cache/node.
# ~/.cache is chowned because mounting a volume at ~/.cache/ms-playwright makes
# Docker create that parent root-owned, which breaks every other user of it —
# corepack included — not just Playwright.
# /cache/pnpm-store is chowned non-recursively and /cache itself is left alone:
# that store is shared with every other dev container on the machine, so
# recursing would be slow and would rewrite other projects' data for no reason.
echo "▶ Fixing cache-volume ownership…"
sudo mkdir -p "$HOME/.gradle" "$HOME/.local/share/pnpm" \
              "$HOME/.cache/ms-playwright" /cache/pnpm-store
sudo chown -R "$(id -u):$(id -g)" "$HOME/.gradle" "$HOME/.local/share/pnpm"
sudo chown "$(id -u):$(id -g)" "$HOME/.cache" "$HOME/.cache/ms-playwright" /cache/pnpm-store

echo "▶ Configuring git hooks (ktfmt format-on-commit)…"
git config core.hooksPath .githooks

echo "▶ Enabling corepack + pnpm (pinned via package.json packageManager field)…"
corepack enable
corepack prepare pnpm@11.8.0 --activate

# Point pnpm at the store shared across every dev container on this machine, so
# a package version is downloaded once per machine rather than once per project.
# One setting covers both frontend/ and landing/ — store-dir is global.
# Only pnpm's global config can do this: pnpm ignores npm_config_store_dir /
# PNPM_STORE_DIR and, as of pnpm 11, ~/.npmrc and the project .npmrc too, so
# devcontainer.json cannot set it. (pnpm-workspace.yaml would work but is
# committed, and would point the host's pnpm at /cache, absent on the host.)
# PNPM_HOME must be on PATH first — `pnpm config set --global` fails outright if
# its global bin dir isn't. And it must run from $HOME: until store-dir is set,
# pnpm falls back to a store on the *project's* drive whenever its default store
# is on another drive (always true here), and this command self-installs the
# pinned pnpm, which would leave a stray .pnpm-store in the repo.
echo "▶ Pointing pnpm at the shared store (/cache/pnpm-store)…"
export PNPM_HOME="$HOME/.local/share/pnpm"
export PATH="$PNPM_HOME/bin:$PATH"
mkdir -p "$PNPM_HOME/bin"
( cd "$HOME" && pnpm config set --global store-dir /cache/pnpm-store )

# CI=true: changing the store location invalidates any existing node_modules
# (pnpm records the store it was built against in .modules.yaml), so pnpm wants
# to purge and rebuild it and asks first. postCreateCommand has no TTY, so that
# prompt is fatal (ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY) — it would break
# this migration itself. Rebuilding node_modules here is expected anyway.
echo "▶ Installing frontend dependencies…"
(cd frontend && CI=true pnpm install)

if [ -d landing ]; then
  echo "▶ Installing landing-page dependencies…"
  (cd landing && CI=true pnpm install)
fi

echo "▶ Warming the Gradle build (resolves backend dependencies)…"
./gradlew classes testClasses --parallel

echo "▶ Installing Playwright browsers + system deps (best effort)…"
if ! (cd frontend && pnpm exec playwright install --with-deps chromium); then
  echo "  ⚠ Playwright --with-deps failed; retrying without system deps."
  (cd frontend && pnpm exec playwright install chromium) || \
    echo "  ⚠ Skipped Playwright browsers — run 'pnpm exec playwright install' later if you need e2e."
fi

echo "✅ Dev container ready. Start everything with:  ./run serve"
