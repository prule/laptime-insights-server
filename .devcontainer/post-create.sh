#!/usr/bin/env bash
#
# One-time provisioning for the laptime-insights dev container.
# Runs automatically after the container is created (postCreateCommand).
# Safe to re-run by hand:  bash .devcontainer/post-create.sh
#
set -euo pipefail

# Named-volume mounts (~/.gradle, pnpm store) are created root-owned on first boot.
# Hand them to the container user so installs don't hit permission errors.
echo "▶ Fixing cache-volume ownership…"
sudo mkdir -p "$HOME/.gradle" "$HOME/.local/share/pnpm"
sudo chown -R "$(id -u):$(id -g)" "$HOME/.gradle" "$HOME/.local/share/pnpm"

echo "▶ Configuring git hooks (ktfmt format-on-commit)…"
git config core.hooksPath .githooks

echo "▶ Enabling corepack + pnpm (pinned via package.json packageManager field)…"
corepack enable
corepack prepare pnpm@11.8.0 --activate

echo "▶ Installing frontend dependencies…"
(cd frontend && pnpm install)

if [ -d landing ]; then
  echo "▶ Installing landing-page dependencies…"
  (cd landing && pnpm install)
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
