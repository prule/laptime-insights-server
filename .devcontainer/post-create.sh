#!/usr/bin/env bash
# One-time provisioning for the laptime-insights dev container.
# Runs automatically after the container is created (postCreateCommand).
# Idempotent — safe to re-run by hand:  bash .devcontainer/post-create.sh
set -euo pipefail

echo "==> laptime-insights dev container: provisioning"

# Named-volume mounts (Playwright browsers, shared pnpm store + Gradle cache) are
# created root-owned on first boot. Hand them to `vscode` so installs don't hit
# permission errors. ~/.cache is chowned because mounting a volume at
# ~/.cache/ms-playwright makes Docker create that parent root-owned, which would
# break every other user of it. The shared /cache/pnpm-store and /cache/gradle
# are chowned NON-recursively and /cache itself is left alone: they are shared
# with every other dev container, so recursing would be slow and rewrite other
# projects' data.
echo "==> Fixing cache-volume ownership"
sudo mkdir -p "$HOME/.cache/ms-playwright" /cache/pnpm-store /cache/gradle
sudo chown vscode:vscode "$HOME/.cache" "$HOME/.cache/ms-playwright" /cache/pnpm-store /cache/gradle

# Make fnm + pnpm available in this non-interactive shell (mirrors ~/.bashrc).
export FNM_DIR="$HOME/.fnm"
export PNPM_HOME="$HOME/.local/share/pnpm"
export PATH="$FNM_DIR:$PNPM_HOME/bin:$PATH"
eval "$(fnm env)"

# Reconcile Node with the repo pin. `.node-version` is the source of truth; if
# it names a patch the image didn't bake in, install it now.
fnm use --install-if-missing
fnm default "$(fnm current)"

echo "==> Toolchain versions"
node --version
pnpm --version
java -version 2>&1 | head -1

echo "==> Configuring git hooks (ktfmt format-on-commit)"
git config core.hooksPath .githooks

# Point pnpm at the store shared across every dev container on this machine, so
# a package version is downloaded once per machine rather than once per project.
# One setting covers both frontend/ and landing/ — store-dir is global. This can
# only be done via pnpm's global config: pnpm ignores npm_config_store_dir /
# PNPM_STORE_DIR and, as of pnpm 11, ~/.npmrc and the project .npmrc too. Run it
# from $HOME: until store-dir is set, pnpm falls back to a store on the
# *project's* drive whenever its default store is on another drive (always true
# here), which is where stray .pnpm-store dirs come from. --global makes cwd
# irrelevant to where the setting lands.
echo "==> Pointing pnpm at the shared store (/cache/pnpm-store)"
( cd "$HOME" && pnpm config set --global store-dir /cache/pnpm-store )

# CI=true: changing the store location invalidates any existing node_modules
# (pnpm records the store it was built against in .modules.yaml), so pnpm wants
# to purge and rebuild it and asks first. postCreateCommand has no TTY, so that
# prompt is fatal (ERR_PNPM_ABORTED_REMOVE_MODULES_DIR_NO_TTY).
echo "==> Installing frontend dependencies"
( cd frontend && CI=true pnpm install --frozen-lockfile )

if [ -d landing ]; then
  echo "==> Installing landing-page dependencies"
  ( cd landing && CI=true pnpm install --frozen-lockfile )
fi

echo "==> Warming the Gradle build (resolves backend dependencies)"
./gradlew classes testClasses --parallel

echo "==> Installing Playwright browsers + system deps (best effort)"
if ! ( cd frontend && pnpm exec playwright install --with-deps chromium ); then
  echo "  ! Playwright --with-deps failed; retrying without system deps."
  ( cd frontend && pnpm exec playwright install chromium ) || \
    echo "  ! Skipped Playwright browsers — run 'pnpm exec playwright install' later if you need e2e."
fi

echo "==> Done. Start everything with:  ./run serve"
